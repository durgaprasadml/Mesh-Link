package com.meshlink.ble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.os.PowerManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.ble.discovery.DiscoveryEngine
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
class BleScannerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discoveryEngine: DiscoveryEngine,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    companion object {
        private const val TAG = "BleScanner"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    val scannedDevices = discoveryEngine.scannedDevices

    private var scanCallback: ScanCallback? = null

    init {
        // Wire up hardware delegates to the Discovery Engine
        discoveryEngine.startScanAction = { startHardwareScan() }
        discoveryEngine.stopScanAction = { stopHardwareScan() }
    }
    
    fun startScanning() {
        discoveryEngine.start()
    }
    
    fun stopScanning() {
        discoveryEngine.stop()
    }

    private fun startHardwareScan() {
        scope.launch {
            if (!settingsRepository.bleScanningEnabled.first() || !settingsRepository.isBleEnabled.first()) {
                MeshLogger.d(TAG, "BLE Scanning disabled in settings. Skipping.")
                return@launch
            }
            
            stopHardwareScan()

            val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return@launch
            if (bluetoothAdapter?.isEnabled != true) {
                MeshLogger.w(TAG, "Bluetooth is disabled; skipping hardware scan")
                return@launch
            }
        
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
            .build()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSave = powerManager.isPowerSaveMode

        val scanMode = if (isPowerSave) {
            ScanSettings.SCAN_MODE_LOW_POWER
        } else {
            ScanSettings.SCAN_MODE_LOW_LATENCY
        }

        val settings = ScanSettings.Builder()
            .setScanMode(scanMode)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::processResult)
            }

            override fun onScanFailed(errorCode: Int) {
                MeshLogger.e(TAG, "BLE scan failed with error code: $errorCode")
                if (errorCode != ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                    scope.launch {
                        if (settingsRepository.bleAutoRestart.first()) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                MeshLogger.d(TAG, "Attempting to restart BLE scan after failure")
                                startHardwareScan()
                            }, 5000L)
                        }
                    }
                }
            }
        }
        
        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            MeshLogger.e(TAG, "SecurityException: Missing BLE scan permission", e)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Exception starting hardware scan: ${e.message}", e)
        }
        }
    }

    private fun stopHardwareScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        try {
            scanCallback?.let {
                scanner.stopScan(it)
                scanCallback = null
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping hardware scan: ${e.message}", e)
        }
    }

    private fun processResult(result: ScanResult) {
        val record = result.scanRecord ?: return
        val uuids = record.serviceUuids ?: emptyList()
        if (!uuids.contains(ParcelUuid(BleConstants.MESH_SERVICE_UUID))) return

        val deviceAddress = result.device.address
        val rssi = result.rssi
        val serviceData = record.getManufacturerSpecificData(BleConstants.MANUFACTURER_ID)
        
        if (serviceData == null || serviceData.size < 12) {
            // Ignore if missing data or old format
            return
        }

        // New payload format:
        // 0-7: Mesh ID bytes
        // 8: Capabilities byte
        // 9-11: Name preview bytes
        val meshIdBytes = ByteArray(8)
        System.arraycopy(serviceData, 0, meshIdBytes, 0, 8)
        val meshId = String(meshIdBytes, Charsets.UTF_8).replace("\u0000", "").trim()
        
        val capabilities = serviceData[8]
        
        val nameBytes = ByteArray(3)
        System.arraycopy(serviceData, 9, nameBytes, 0, 3)
        var name = String(nameBytes, Charsets.UTF_8).replace("\u0000", "").trim()
        
        if (name.isBlank()) {
            name = record.deviceName ?: result.device.name ?: "Peer"
        }
        
        // Pass to Discovery Engine
        discoveryEngine.onDeviceDiscovered(
            macAddress = deviceAddress,
            meshId = meshId,
            name = name,
            rssi = rssi,
            capabilities = capabilities
        )
    }
}
