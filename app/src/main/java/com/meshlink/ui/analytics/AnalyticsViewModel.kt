package com.meshlink.ui.analytics

import androidx.lifecycle.ViewModel
import com.meshlink.analytics.data.MeshAnalytics
import com.meshlink.analytics.data.MeshStats
import com.meshlink.routing.data.MeshRouter
import com.meshlink.analytics.data.RelayLogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

data class AnalyticsUiState(
    val stats: MeshStats = MeshStats(),
    val recentLog: List<RelayLogEntry> = emptyList(),
    val activeNodes: Set<String> = emptySet(),
    val hopDistribution: Map<Int, Int> = emptyMap(),
    val routeTableSize: Int = 0,
    val routeTable: Map<String, String> = emptyMap()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analytics: MeshAnalytics,
    private val meshRouter: MeshRouter
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        analytics.stats,
        analytics.recentRelayLog,
        analytics.activeNodes,
        analytics.hopDistribution
    ) { stats, recentLog, activeNodes, hopDistribution ->
        AnalyticsUiState(
            stats = stats,
            recentLog = recentLog,
            activeNodes = activeNodes,
            hopDistribution = hopDistribution,
            routeTableSize = meshRouter.routeTable.size,
            routeTable = meshRouter.routeTable.mapValues { it.value.nextHop }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}
