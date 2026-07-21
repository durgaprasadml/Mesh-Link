package com.meshlink.ble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.os.PowerManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
@SuppressLint("MissingPermission")
class BleAdvertiserManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    companion object {
        private const val TAG = "BleAdvertiser"
        private const val NAME_PREVIEW_LENGTH = 3
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var advertiseCallback: AdvertiseCallback? = null

    fun startAdvertising(name: String, meshId: String, capabilities: Byte = 0) {
        scope.launch {
            if (!settingsRepository.bleAdvertisingEnabled.first() || !settingsRepository.isBleEnabled.first()) {
                MeshLogger.d(TAG, "BLE Advertising disabled in settings. Skipping.")
                return@launch
            }

            stopAdvertising()

            val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return@launch
            if (bluetoothAdapter?.isEnabled != true) {
                MeshLogger.w(TAG, "Bluetooth is disabled; skipping advertising")
                return@launch
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isPowerSave = powerManager.isPowerSaveMode
            val userTxPower = settingsRepository.bleTxPower.first()

            val advMode = if (isPowerSave) {
                AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
            } else {
                AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
            }

            val txPower = if (isPowerSave) {
                AdvertiseSettings.ADVERTISE_TX_POWER_LOW
            } else {
                when (userTxPower) {
                    0 -> AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW
                    1 -> AdvertiseSettings.ADVERTISE_TX_POWER_LOW
                    2 -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
                    3 -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                    else -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
                }
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(advMode)
                .setTxPowerLevel(txPower)
                .setConnectable(true)
                .build()

        // Legacy BLE advertising is capped at 31 bytes per packet.
        // We pack 8 bytes Mesh ID, 1 byte Capabilities, 3 bytes Name preview
        val meshIdBytes = BleConstants.toNetworkId(meshId).toByteArray(Charsets.UTF_8).copyOf(8)
        val nameBytes = name.take(NAME_PREVIEW_LENGTH).padEnd(NAME_PREVIEW_LENGTH, ' ').toByteArray(Charsets.UTF_8).copyOf(3)
        val combinedData = ByteArray(12)
        System.arraycopy(meshIdBytes, 0, combinedData, 0, 8)
        combinedData[8] = capabilities
        System.arraycopy(nameBytes, 0, combinedData, 9, 3)

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(BleConstants.MANUFACTURER_ID, combinedData)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                MeshLogger.d(TAG, "Advertising started")
            }
            override fun onStartFailure(errorCode: Int) {
                MeshLogger.e(TAG, "Advertising failed with error code: $errorCode")
                if (errorCode != AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                    scope.launch {
                        if (settingsRepository.bleAutoRestart.first()) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                MeshLogger.d(TAG, "Attempting to restart BLE advertising after failure")
                                startAdvertising(name, meshId, capabilities)
                            }, 5000L)
                        }
                    }
                }
            }
        }
        try {
            advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)
        } catch (e: SecurityException) {
            MeshLogger.e(TAG, "SecurityException: Missing BLE advertise permission", e)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Exception starting advertising: ${e.message}", e)
            scope.launch {
                if (settingsRepository.bleAutoRestart.first()) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        MeshLogger.d(TAG, "Attempting to restart BLE advertising after exception")
                        startAdvertising(name, meshId, capabilities)
                    }, 5000L)
                }
            }
        }
        }
    }

    fun stopAdvertising() {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        try {
            advertiseCallback?.let {
                advertiser.stopAdvertising(it)
                advertiseCallback = null
                MeshLogger.d(TAG, "Advertising stopped")
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping advertising: ${e.message}", e)
        }
    }
}
