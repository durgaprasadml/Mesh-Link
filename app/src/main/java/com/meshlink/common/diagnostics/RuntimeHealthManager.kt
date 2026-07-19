package com.meshlink.common.diagnostics

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class HealthScore {
    EXCELLENT, GOOD, WARNING, CRITICAL
}

@Singleton
class RuntimeHealthManager @Inject constructor(
    private val diagnosticsManager: DiagnosticsManager,
    private val resourceMonitor: SystemResourceMonitor
) {

    private val monitorScope = CoroutineScope(Dispatchers.Default)

    private val _healthScore = MutableStateFlow(HealthScore.EXCELLENT)
    val healthScore: StateFlow<HealthScore> = _healthScore

    init {
        monitorScope.launch {
            // Observe resources and diagnostics to compute health score
            combine(diagnosticsManager.healthState, resourceMonitor.metrics) { diag, res ->
                computeHealthScore(diag, res)
            }.collect { score ->
                _healthScore.value = score
                diagnosticsManager.updateHealthState { state ->
                    state.copy(healthScore = score.name)
                }
            }
        }
    }

    fun startMonitoring() {
        monitorScope.launch {
            while (true) {
                // Periodically update basic system memory usage
                resourceMonitor.updateMetrics()
                
                val currentMetrics = resourceMonitor.metrics.value
                diagnosticsManager.updateHealthState { state ->
                    state.copy(
                        memoryUsageMb = currentMetrics.jvmHeapUsageMb + currentMetrics.nativeMemoryUsageMb
                    )
                }
                
                delay(60_000) // Poll every 1 minute
            }
        }
    }

    private fun computeHealthScore(
        diag: DiagnosticsManager.SystemHealthState,
        res: SystemResourceMonitor.ResourceMetrics
    ): HealthScore {
        if (res.isCriticalMemory || !diag.isDatabaseHealthy) {
            return HealthScore.CRITICAL
        }
        
        var warningCount = 0
        if (!diag.isBleHealthy) warningCount++
        if (!diag.isWifiHealthy) warningCount++
        if (res.jvmHeapUsageMb > 256) warningCount++ // Arbitrary high memory threshold
        
        return when {
            warningCount >= 2 -> HealthScore.CRITICAL
            warningCount == 1 -> HealthScore.WARNING
            res.jvmHeapUsageMb > 128 -> HealthScore.GOOD
            else -> HealthScore.EXCELLENT
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

    fun reportDatabaseHealthy() {
        diagnosticsManager.updateHealthState { it.copy(isDatabaseHealthy = true) }
    }

    fun reportActivePeers(count: Int) {
        diagnosticsManager.updateHealthState { it.copy(activePeersCount = count) }
    }
}
