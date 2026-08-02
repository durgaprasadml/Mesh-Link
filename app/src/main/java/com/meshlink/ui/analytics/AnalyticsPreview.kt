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
        activeRoutesCount = 8,
        activeSessionsCount = 3
    ),
    connectionQuality = ConnectionQualityUi(
        isBleActive = true,
        isWifiDirectActive = true,
        signalQualityPercent = 88,
        meshStateText = "OPERATIONAL",
        bleSignalDbm = -62,
        wifiDirectSpeedMbps = 48f,
        meshStabilityPercent = 96
    ),
    packetStats = PacketStatisticsUi(
        sent = 142,
        delivered = 138,
        relayed = 45,
        failed = 4,
        broadcasts = 12,
        deliveryRatePercent = 97.1f,
        avgHops = 1.4
    ),
    transferStats = TransferAnalyticsUi(
        filesSentCount = 24,
        filesReceivedCount = 18,
        totalBytesTransferred = 104857600L,
        averageSpeedKbps = 2450f,
        successRatePercent = 98.5f,
        activeTransfersCount = 1
    ),
    batteryImpact = BatteryImpactUi(
        batteryLevelText = "88%",
        impactLevel = "LOW",
        isPowerSaveActive = false,
        bleUsagePercent = 60,
        wifiUsagePercent = 40,
        cpuUsagePercent = 12,
        memoryUsageMb = 42,
        backgroundActivityText = "Active (Mesh Relay Engine)"
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
    timelineEvents = defaultSampleTimelineEvents()
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

@Preview(name = "Foldable Layout", device = "spec:width=673dp,height=841dp", showBackground = true)
@Composable
fun AnalyticsPreviewFoldable() {
    MeshTheme {
        MeshAnalyticsScreen(
            state = sampleState,
            onBackClick = {}
        )
    }
}

@Preview(name = "Landscape Mobile", device = "spec:width=891dp,height=411dp,orientation=landscape", showBackground = true)
@Composable
fun AnalyticsPreviewLandscape() {
    MeshTheme {
        MeshAnalyticsScreen(
            state = sampleState,
            onBackClick = {}
        )
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
fun AnalyticsPreviewEmpty() {
    MeshTheme {
        NoAnalytics(onExploreClick = {})
    }
}
