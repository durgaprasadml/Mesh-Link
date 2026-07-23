package com.meshlink.scalability.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EnterpriseScalabilityMetrics(
    val averageLatencyMs: Long = 0,
    val worstLatencyMs: Long = 0,
    val broadcastEfficiency: Float = 1.0f,
    val discoveryEfficiency: Float = 1.0f,
    val workerUtilizationPercent: Int = 0
)

@Singleton
class ScalabilityMetricsManager @Inject constructor() {

    private val _metrics = MutableStateFlow(EnterpriseScalabilityMetrics())
    val metrics: StateFlow<EnterpriseScalabilityMetrics> = _metrics

    fun updateMetrics(modifier: (EnterpriseScalabilityMetrics) -> EnterpriseScalabilityMetrics) {
        _metrics.value = modifier(_metrics.value)
    }

    fun recordLatency(latencyMs: Long) {
        val current = _metrics.value
        val newAvg = if (current.averageLatencyMs == 0L) latencyMs else (current.averageLatencyMs + latencyMs) / 2
        val newWorst = maxOf(current.worstLatencyMs, latencyMs)
        
        updateMetrics { it.copy(averageLatencyMs = newAvg, worstLatencyMs = newWorst) }
    }
}
