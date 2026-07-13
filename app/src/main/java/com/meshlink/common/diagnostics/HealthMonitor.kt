package com.meshlink.common.diagnostics

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class HealthMonitor @Inject constructor(
    private val diagnosticsManager: DiagnosticsManager
) {

    private val monitorScope = CoroutineScope(Dispatchers.Default)

    fun startMonitoring() {
        monitorScope.launch {
            while (true) {
                // Periodically update basic system memory usage
                val memoryUsageMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
                
                diagnosticsManager.updateHealthState { state ->
                    state.copy(memoryUsageMb = memoryUsageMb.toInt())
                }
                
                delay(60_000) // Poll every 1 minute
            }
        }
    }

    fun reportBleFailure() {
        diagnosticsManager.updateHealthState { it.copy(isBleHealthy = false) }
    }

    fun reportBleHealthy() {
        diagnosticsManager.updateHealthState { it.copy(isBleHealthy = true) }
    }

    fun reportWifiFailure() {
        diagnosticsManager.updateHealthState { it.copy(isWifiHealthy = false) }
    }

    fun reportWifiHealthy() {
        diagnosticsManager.updateHealthState { it.copy(isWifiHealthy = true) }
    }

    fun reportDatabaseFailure() {
        diagnosticsManager.updateHealthState { it.copy(isDatabaseHealthy = false) }
    }

    fun reportActivePeers(count: Int) {
        diagnosticsManager.updateHealthState { it.copy(activePeersCount = count) }
    }
}
