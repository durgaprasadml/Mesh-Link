package com.meshlink.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.RouteEntry
import com.meshlink.recovery.engine.MeshHealthMetrics
import com.meshlink.recovery.engine.MeshReliabilityManager
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.RoutingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutingDiagnosticsUiState(
    val routes: List<RouteEntry> = emptyList(),
    val meshSize: Int = 0,
    val activeDiscoveryCount: Int = 0,
    val pendingQueueSize: Int = 0,
    val storeForwardCount: Int = 0,
    val duplicateCacheSize: Int = 0,
    val routeRepairCount: Int = 0,
    val networkHealth: String = "HEALTHY",
    val healthScore: Int = 100,
    val partitionEvents: Int = 0,
    val batteryImpact: String = "LOW"
)

@HiltViewModel
class RoutingDiagnosticsViewModel @Inject constructor(
    private val router: Router,
    private val routingEngine: RoutingEngine,
    private val reliabilityManager: MeshReliabilityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutingDiagnosticsUiState())
    val uiState: StateFlow<RoutingDiagnosticsUiState> = _uiState.asStateFlow()

    init {
        startMetricsPolling()
    }

    private fun startMetricsPolling() {
        viewModelScope.launch {
            reliabilityManager.healthMetrics.collect { metrics ->
                val routesList = routingEngine.routeManager.routeCache.getAllDestinations().flatMap { dest ->
                    routingEngine.routeManager.routeCache.getRoutesForDestination(dest)
                }

                val healthStatus = when {
                    metrics.networkHealthScore < 50 -> "DEGRADED"
                    metrics.networkHealthScore < 75 -> "MODERATE"
                    routesList.isEmpty() -> "STANDBY"
                    else -> "HEALTHY"
                }

                _uiState.value = RoutingDiagnosticsUiState(
                    routes = routesList,
                    meshSize = metrics.meshSize,
                    activeDiscoveryCount = routingEngine.discoveryEngine.discoveryCount.value,
                    pendingQueueSize = metrics.pendingQueueSize,
                    storeForwardCount = reliabilityManager.storeAndForwardManager.queuedCount.value,
                    duplicateCacheSize = routingEngine.getDuplicateCacheSize(),
                    routeRepairCount = routingEngine.repairManager.repairCount.value,
                    networkHealth = healthStatus,
                    healthScore = metrics.networkHealthScore,
                    partitionEvents = metrics.partitionEvents,
                    batteryImpact = metrics.batteryImpact
                )
            }
        }
    }
}
