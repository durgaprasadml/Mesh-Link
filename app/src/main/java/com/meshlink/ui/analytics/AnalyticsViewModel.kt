package com.meshlink.ui.analytics

import androidx.lifecycle.ViewModel
import com.meshlink.data.analytics.MeshAnalytics
import com.meshlink.data.ble.MeshRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analytics: MeshAnalytics,
    private val meshRouter: MeshRouter
) : ViewModel() {

    val stats = analytics.stats
    val recentLog = analytics.recentRelayLog
    val activeNodes = analytics.activeNodes
    val hopDistribution = analytics.hopDistribution

    val routeTableSize: Int
        get() = meshRouter.routeTable.size

    val routeTable: Map<String, String>
        get() = meshRouter.routeTable.toMap()
}
