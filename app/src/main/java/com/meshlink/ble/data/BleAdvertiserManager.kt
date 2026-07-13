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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class BleAdvertiserManager @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val TAG = "BleAdvertiser"
        private const val NAME_PREVIEW_LENGTH = 3
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var advertiseCallback: AdvertiseCallback? = null

    fun startAdvertising(name: String, meshId: String) {
        stopAdvertising()

        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        if (bluetoothAdapter?.isEnabled != true) {
            MeshLogger.w(TAG, "Bluetooth is disabled; skipping advertising")
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSave = powerManager.isPowerSaveMode

        val advMode = if (isPowerSave) {
            AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
        } else {
            AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
        }

        val txPower = if (isPowerSave) {
            AdvertiseSettings.ADVERTISE_TX_POWER_LOW
        } else {
            AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(advMode)
            .setTxPowerLevel(txPower)
            .setConnectable(true)
            .build()

        // Legacy BLE advertising is capped at 31 bytes per packet. With a 128-bit UUID,
        // only a very small preview fits in service data without triggering DATA_TOO_LARGE.
        val meshIdPart = BleConstants.toNetworkId(meshId)
        val namePart = name.take(NAME_PREVIEW_LENGTH)
        val combinedData = "$meshIdPart|$namePart".toByteArray(Charsets.UTF_8)

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(ParcelUuid(BleConstants.MESH_SERVICE_UUID), combinedData)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                MeshLogger.d(TAG, "Advertising started")
            }
            override fun onStartFailure(errorCode: Int) {
                MeshLogger.e(TAG, "Advertising failed with error code: $errorCode")
            }
        }
        advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)
    }

    fun stopAdvertising() {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        advertiseCallback?.let {
            advertiser.stopAdvertising(it)
            advertiseCallback = null
            MeshLogger.d(TAG, "Advertising stopped")
        }
    }
}
