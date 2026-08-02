package com.meshlink.ui.analytics

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.analytics.data.LogType
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteMetrics
import com.meshlink.domain.model.RouteState
import com.meshlink.domain.model.RouteType
import com.meshlink.ui.designsystem.theme.MeshTheme

private val sampleState = TacticalAnalyticsUiState(
    health = MeshHealthUi(
        networkHealth = "HEALTHY",
        healthScore = 95,
        activeNodesCount = 6,
        connectedPeersCount = 4,
        relayNodesCount = 2,
        activeRoutesCount = 8
    ),
    connectionQuality = ConnectionQualityUi(
        isBleActive = true,
        isWifiDirectActive = true,
        signalQualityPercent = 88,
        meshStateText = "OPERATIONAL"
    ),
    packetStats = PacketStatisticsUi(
        sent = 142,
        delivered = 138,
        relayed = 45,
        failed = 4,
        deliveryRatePercent = 97.1f,
        avgHops = 1.4
    ),
    hopDistribution = mapOf(1 to 85, 2 to 35, 3 to 12, 4 to 3),
    activeNodes = setOf("node_alpha", "node_bravo", "node_charlie", "node_delta"),
    routes = listOf(
        RouteEntry(
            destinationId = "node_alpha",
            nextHop = "node_bravo",
            hops = 2,
            score = 85,
            routeType = RouteType.HYBRID,
            state = RouteState.ACTIVE,
            metrics = RouteMetrics(rssi = -65, averageLatencyMs = 45L, packetLossRate = 0.02f)
        )
    ),
    events = listOf(
        DiagnosticEventUi(
            id = "evt_1",
            timestamp = System.currentTimeMillis() - 120000L,
            title = "Peer Connected",
            detail = "Node alpha connected via BLE Mesh",
            category = LogType.RELAY
        ),
        DiagnosticEventUi(
            id = "evt_2",
            timestamp = System.currentTimeMillis() - 60000L,
            title = "Route Updated",
            detail = "Dynamic 2-hop route to node_charlie established",
            category = LogType.SECURITY
        )
    ),
    logEntries = listOf(
        LogEntryUi(
            id = "log_1",
            timestamp = System.currentTimeMillis(),
            level = "INFO",
            tag = "MeshRouter",
            message = "Relayed packet #1048 to destination node_charlie",
            rawLogType = LogType.RELAY
        )
    )
)

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun AnalyticsPreviewLight() {
    MeshTheme(themeMode = "LIGHT") {
        MeshAnalyticsScreen(
            state = sampleState,
            onBackClick = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AnalyticsPreviewDark() {
    MeshTheme(themeMode = "DARK") {
        MeshAnalyticsScreen(
            state = sampleState,
            onBackClick = {}
        )
    }
}

@Preview(name = "AMOLED Mode", showBackground = true)
@Composable
fun AnalyticsPreviewAmoled() {
    MeshTheme(themeMode = "AMOLED") {
        MeshAnalyticsScreen(
            state = sampleState,
            onBackClick = {}
        )
    }
}

@Preview(name = "Tablet Layout", device = "spec:width=1280dp,height=800dp", showBackground = true)
@Composable
fun AnalyticsPreviewTablet() {
    MeshTheme {
        MeshAnalyticsScreen(
            state = sampleState,
            onBackClick = {}
        )
    }
}
