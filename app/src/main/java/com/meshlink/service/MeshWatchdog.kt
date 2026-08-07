package com.meshlink.service

import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.wifi.manager.WifiDirectManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class MeshWatchdog @Inject constructor(
    private val meshSupervisor: MeshSupervisor,
    private val bleScannerManager: BleScannerManager,
    private val bleAdvertiserManager: BleAdvertiserManager,
    private val wifiDirectManager: WifiDirectManager,
    private val transportDiagnostics: TransportDiagnostics,
    @com.meshlink.di.ApplicationScope private val externalScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MeshWatchdog"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private val BACKOFF_SCHEDULE_MS = listOf(1000L, 2000L, 5000L, 10000L, 20000L, 30000L, 60000L)
    }

    private var heartbeatJob: Job? = null
    private val failureCounts = mutableMapOf<RadioSubsystem, Int>()
    private var recoveryCount: Int = 0

    fun getRecoveryCount(): Int = recoveryCount

    fun start() {
        if (heartbeatJob?.isActive == true) return
        MeshLogger.d(TAG, "Starting Mesh Watchdog heartbeat service")

        heartbeatJob = externalScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                performHealthCheck()
            }
        }
    }

    fun stop() {
        MeshLogger.d(TAG, "Stopping Mesh Watchdog heartbeat service")
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    suspend fun performHealthCheck() {
        val states = meshSupervisor.subsystemStates.value

        // Check BLE Scanner
        if (!bleScannerManager.isScanning && states[RadioSubsystem.BLE_SCANNER] == RadioState.RUNNING) {
            handleSubsystemFailure(RadioSubsystem.BLE_SCANNER, "Scanner stopped unexpectedly")
        } else if (bleScannerManager.isScanning) {
            failureCounts[RadioSubsystem.BLE_SCANNER] = 0
        }

        // Check BLE Advertiser
        if (!bleAdvertiserManager.isAdvertising && states[RadioSubsystem.BLE_ADVERTISER] == RadioState.RUNNING) {
            handleSubsystemFailure(RadioSubsystem.BLE_ADVERTISER, "Advertiser stopped unexpectedly")
        } else if (bleAdvertiserManager.isAdvertising) {
            failureCounts[RadioSubsystem.BLE_ADVERTISER] = 0
        }

        // Check Wi-Fi Direct
        if (wifiDirectManager.radioState.value == RadioState.FAILED) {
            handleSubsystemFailure(RadioSubsystem.WIFI_DIRECT, "Wi-Fi Direct in FAILED state")
        } else if (wifiDirectManager.radioState.value == RadioState.RUNNING) {
            failureCounts[RadioSubsystem.WIFI_DIRECT] = 0
        }

        MeshLogger.d(TAG, "Watchdog heartbeat check complete. System operational: ${meshSupervisor.isFullyOperational()}")
    }

    private suspend fun handleSubsystemFailure(subsystem: RadioSubsystem, reason: String) {
        val count = failureCounts.getOrDefault(subsystem, 0) + 1
        failureCounts[subsystem] = count
        recoveryCount++

        val backoffIndex = (count - 1).coerceAtMost(BACKOFF_SCHEDULE_MS.lastIndex)
        val backoffDelay = BACKOFF_SCHEDULE_MS[backoffIndex]

        MeshLogger.w(TAG, "Watchdog detected failure in $subsystem ($reason). Scheduling recovery attempt #$count with backoff ${backoffDelay}ms")
        
        delay(backoffDelay)
        meshSupervisor.restartSubsystem(subsystem)
    }
}
