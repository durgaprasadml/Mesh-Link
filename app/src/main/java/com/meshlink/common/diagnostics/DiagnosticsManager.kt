package com.meshlink.common.diagnostics

import com.meshlink.common.logger.EventTimeline
import com.meshlink.common.metrics.MetricsManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

@Singleton
class DiagnosticsManager @Inject constructor(
    private val metricsManager: MetricsManager,
    private val eventTimeline: EventTimeline
) {
    
    private val _healthState = MutableStateFlow(SystemHealthState())
    val healthState: StateFlow<SystemHealthState> = _healthState

    data class SystemHealthState(
        val isBleHealthy: Boolean = true,
        val isWifiHealthy: Boolean = true,
        val isDatabaseHealthy: Boolean = true,
        val activePeersCount: Int = 0,
        val foregroundServiceUptime: Long = 0L,
        val memoryUsageMb: Int = 0
    )

    fun updateHealthState(modifier: (SystemHealthState) -> SystemHealthState) {
        _healthState.value = modifier(_healthState.value)
    }

    fun exportDiagnosticsJson(): String {
        val json = JSONObject()
        
        // 1. Core Health
        val health = JSONObject()
        health.put("isBleHealthy", _healthState.value.isBleHealthy)
        health.put("isWifiHealthy", _healthState.value.isWifiHealthy)
        health.put("isDatabaseHealthy", _healthState.value.isDatabaseHealthy)
        health.put("activePeersCount", _healthState.value.activePeersCount)
        json.put("health", health)

        // 2. Metrics
        val metricsJson = JSONObject()
        metricsManager.getAllMetrics().forEach { (k, v) ->
            metricsJson.put(k, v)
        }
        json.put("metrics", metricsJson)

        // 3. Event Timeline
        val eventsArray = org.json.JSONArray()
        eventTimeline.getEvents().forEach { event ->
            val eventJson = JSONObject()
            eventJson.put("timestamp", event.timestamp)
            eventJson.put("name", event.eventName)
            eventJson.put("details", event.details ?: "")
            eventsArray.put(eventJson)
        }
        json.put("timeline", eventsArray)

        return json.toString(4) // Pretty print with indent 4
    }
}
