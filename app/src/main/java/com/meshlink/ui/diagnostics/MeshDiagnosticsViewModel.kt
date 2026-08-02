package com.meshlink.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.routing.engine.MeshTopologyManager
import com.meshlink.routing.engine.RoutingTable
import com.meshlink.routing.engine.TopologyMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DiagnosticsUiState(
    val activeRoutesCount: Int = 0,
    val directNeighborsCount: Int = 0,
    val topologyMetrics: TopologyMetrics = TopologyMetrics(),
    val reachabilityList: List<com.meshlink.routing.engine.ReachableNode> = emptyList(),
    val routesMap: Map<String, List<com.meshlink.domain.model.RouteEntry>> = emptyMap()
)

@HiltViewModel
class MeshDiagnosticsViewModel @Inject constructor(
    private val topologyManager: MeshTopologyManager,
    private val routingTable: RoutingTable
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        topologyManager.metrics,
        topologyManager.reachableNodes,
        _refreshTrigger
    ) { metrics, reachable, _ ->
        val routes = routingTable.getAllRoutes()
        DiagnosticsUiState(
            activeRoutesCount = routes.values.sumOf { it.size },
            directNeighborsCount = metrics.directNeighbors,
            topologyMetrics = metrics,
            reachabilityList = reachable,
            routesMap = routes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiagnosticsUiState())

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
        topologyManager.recomputeTopology()
    }
}
