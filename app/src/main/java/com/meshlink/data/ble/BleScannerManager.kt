package com.meshlink.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.meshlink.domain.model.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleScannerManager(private val context: Context) {
    companion object {
        private const val TAG = "BleScanner"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val _scannedDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    val scannedDevices: StateFlow<Map<String, BleDevice>> = _scannedDevices.asStateFlow()
    
    private val lastSeenMap = ConcurrentHashMap<String, Long>()

    private var scanCallback: ScanCallback? = null
    private var cleanupJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startScanning() {
        stopScanning()

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "Bluetooth is disabled; skipping scan")
            return
        }
        
        // FIX Issue 3: Do NOT reset scannedDevices on scan restart.
        // The stale device cleanup coroutine below handles removing
        // devices not seen for 15 seconds. Resetting here was causing
        // the connection status to flicker to OFFLINE on every scan cycle.
        
        // Use an empty filter to ensure aggressive discovery to catch all packets.
        // We will manually identify our Mesh UUID in the results to bypass OEM filter drop bugs.
        val filter = ScanFilter.Builder().build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::processResult)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code: $errorCode")
            }
        }
        
        scanner.startScan(listOf(filter), settings, scanCallback)
        
        // Stale Device Cleanup Coroutine Loop & Scan Timeout Recovery
        cleanupJob = scope.launch {
            var lastScanRestartTime = System.currentTimeMillis()
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                val keysToRemove = lastSeenMap.entries
                    .filter { now - it.value > 15000 }
                    .map { it.key }
                
                if (keysToRemove.isNotEmpty()) {
                    keysToRemove.forEach { lastSeenMap.remove(it) }
                    _scannedDevices.update { current ->
                        current.filterKeys { it !in keysToRemove }
                    }
                }

                // FIX: Scan timeout recovery - Android downgrades scans >30 mins to opportunistic.
                // Restart scan every 15 minutes to keep it in low latency mode continuously.
                if (now - lastScanRestartTime > 15 * 60 * 1000L) {
                    Log.d(TAG, "15-minute scan refresh to avoid opportunistic downgrade.")
                    lastScanRestartTime = now
                    try {
                        scanCallback?.let {
                            scanner.stopScan(it)
                            delay(1000)
                            scanner.startScan(listOf(filter), settings, it)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh scan: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopScanning() {
        cleanupJob?.cancel()
        cleanupJob = null
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        scanCallback?.let {
            scanner.stopScan(it)
            scanCallback = null
        }
    }

    private fun processResult(result: ScanResult) {
        val record = result.scanRecord ?: return
        val uuids = record.serviceUuids ?: emptyList()
        if (!uuids.contains(ParcelUuid(BleConstants.MESH_SERVICE_UUID))) return

        val deviceAddress = result.device.address
        val rssi = result.rssi
        val serviceData = record.getServiceData(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
        val dataString = serviceData
            ?.takeIf { it.isNotEmpty() }
            ?.toString(Charsets.UTF_8)
            
        if (dataString == null) {
            // Ignore incomplete scans (e.g. passive scan hits without scanResponse data)
            // We MUST have the meshId to route messages to them.
            return
        }

        val parts = dataString.split("|", limit = 2)
        val meshPreview = parts.getOrNull(0)
            ?.ifBlank { null }
            ?.let(BleConstants::toNetworkId)
            ?: return // Must have a meshId

        val displayName = parts.getOrNull(1)?.ifBlank { null }
            ?: record.deviceName
            ?: result.device.name
            ?: "Peer ${meshPreview.takeLast(5)}"

        val bleDevice = BleDevice(
            meshId = meshPreview,
            name = displayName,
            address = deviceAddress,
            rssi = rssi
        )

        lastSeenMap[meshPreview] = System.currentTimeMillis()
        _scannedDevices.update { current ->
            val mutableMap = current.toMutableMap()
            mutableMap[meshPreview] = bleDevice
            mutableMap
        }
    }
}
