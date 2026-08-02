package com.meshlink.ui.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.analytics.data.LogType
import com.meshlink.ui.analytics.DiagnosticEventUi
import com.meshlink.ui.analytics.LogEntryUi
import com.meshlink.ui.analytics.MeshDiagnosticsScreen

@Composable
fun RoutingDiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: RoutingDiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val diagnosticEvents = uiState.routes.mapIndexed { idx, route ->
        DiagnosticEventUi(
            id = "route_${idx}_${route.destinationId}",
            timestamp = System.currentTimeMillis(),
            title = "Route to ${com.meshlink.util.MeshIdNormalizer.canonicalize(route.destinationId)}",
            detail = "NextHop: ${com.meshlink.util.MeshIdNormalizer.canonicalize(route.nextHop)} (${route.hops} hop) | RSSI: ${route.metrics.rssi} dBm",
            category = LogType.SECURITY,
            nodeId = route.destinationId
        )
    }

    val diagnosticLogs = mutableListOf<LogEntryUi>().apply {
        add(
            LogEntryUi(
                id = "health_log",
                timestamp = System.currentTimeMillis(),
                level = uiState.networkHealth,
                tag = "RoutingEngine",
                message = "Network Health: ${uiState.networkHealth} | Score: ${uiState.healthScore} | Mesh Size: ${uiState.meshSize}",
                rawLogType = LogType.RELAY
            )
        )
        add(
            LogEntryUi(
                id = "discovery_log",
                timestamp = System.currentTimeMillis(),
                level = "INFO",
                tag = "RREQEngine",
                message = "Active RREQ: ${uiState.activeDiscoveryCount} | Pending Queue: ${uiState.pendingQueueSize} | Store&Forward: ${uiState.storeForwardCount}",
                rawLogType = LogType.RELAY
            )
        )
        uiState.routes.forEachIndexed { idx, route ->
            add(
                LogEntryUi(
                    id = "route_entry_$idx",
                    timestamp = System.currentTimeMillis(),
                    level = "ROUTE",
                    tag = "RouteCache",
                    message = "Dest: ${com.meshlink.util.MeshIdNormalizer.canonicalize(route.destinationId)} -> Next: ${com.meshlink.util.MeshIdNormalizer.canonicalize(route.nextHop)} | Score: ${route.score}/100",
                    rawLogType = LogType.SECURITY
                )
            )
        }
    }

    MeshDiagnosticsScreen(
        title = "Routing Engine Diagnostics",
        events = diagnosticEvents,
        logs = diagnosticLogs,
        onBackClick = onBackClick
    )
}
