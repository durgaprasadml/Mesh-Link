package com.meshlink.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.database.data.local.RelayDao
import com.meshlink.domain.model.RouteEntry
import com.meshlink.routing.api.Router
import com.meshlink.routing.data.MeshRouter
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
    val networkHealth: String = "HEALTHY"
)

@HiltViewModel
class RoutingDiagnosticsViewModel @Inject constructor(
    private val router: Router,
    private val routingEngine: RoutingEngine,
    private val relayDao: RelayDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutingDiagnosticsUiState())
    val uiState: StateFlow<RoutingDiagnosticsUiState> = _uiState.asStateFlow()

    init {
        startMetricsPolling()
    }

    private fun startMetricsPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val routesList = routingEngine.routeManager.routeCache.getAllDestinations().flatMap { dest ->
                        routingEngine.routeManager.routeCache.getRoutesForDestination(dest)
                    }

                    val sfCount = relayDao.getAllRelayPackets().size
                    val activeRreq = routingEngine.discoveryEngine.discoveryCount.value
                    val pendingQ = routingEngine.discoveryEngine.pendingQueueSize.value
                    val dupSize = routingEngine.getDuplicateCacheSize()
                    val repairs = routingEngine.repairManager.repairCount.value

                    val health = when {
                        routesList.any { it.score < 30 } -> "DEGRADED"
                        routesList.isEmpty() -> "STANDBY"
                        else -> "HEALTHY"
                    }

                    _uiState.value = RoutingDiagnosticsUiState(
                        routes = routesList,
                        meshSize = routesList.map { it.destinationId }.distinct().size,
                        activeDiscoveryCount = activeRreq,
                        pendingQueueSize = pendingQ,
                        storeForwardCount = sfCount,
                        duplicateCacheSize = dupSize,
                        routeRepairCount = repairs,
                        networkHealth = health
                    )
                } catch (e: Exception) {
                    // Ignore transient exceptions during polling
                }
                delay(1000L) // Refresh diagnostics every second
            }
        }
    }
}
