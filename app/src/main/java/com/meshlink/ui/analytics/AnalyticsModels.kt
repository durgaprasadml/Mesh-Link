package com.meshlink.ui.analytics

import androidx.compose.runtime.Immutable
import com.meshlink.analytics.data.LogType
import com.meshlink.analytics.data.MeshStats
import com.meshlink.analytics.data.RelayLogEntry
import com.meshlink.domain.model.RouteEntry

@Immutable
data class MeshHealthUi(
    val networkHealth: String = "HEALTHY",
    val healthScore: Int = 100,
    val activeNodesCount: Int = 0,
    val connectedPeersCount: Int = 0,
    val relayNodesCount: Int = 0,
    val activeRoutesCount: Int = 0,
    val activeSessionsCount: Int = 3,
    val isEncryptionActive: Boolean = true
)

@Immutable
data class ConnectionQualityUi(
    val isBleActive: Boolean = true,
    val isWifiDirectActive: Boolean = true,
    val signalQualityPercent: Int = 92,
    val meshStateText: String = "OPERATIONAL",
    val encryptionStatusText: String = "AES-256-GCM / E2EE Active",
    val bleSignalDbm: Int = -62,
    val wifiDirectSpeedMbps: Float = 48f,
    val meshStabilityPercent: Int = 96
)

@Immutable
data class PacketStatisticsUi(
    val sent: Int = 0,
    val delivered: Int = 0,
    val relayed: Int = 0,
    val failed: Int = 0,
    val broadcasts: Int = 0,
    val deliveryRatePercent: Float = 0f,
    val avgHops: Double = 0.0
)

@Immutable
data class TransferAnalyticsUi(
    val filesSentCount: Int = 24,
    val filesReceivedCount: Int = 18,
    val totalBytesTransferred: Long = 104857600L,
    val averageSpeedKbps: Float = 2450f,
    val successRatePercent: Float = 98.5f,
    val activeTransfersCount: Int = 1
)

@Immutable
data class RoutingNodeUi(
    val id: String,
    val label: String,
    val isLocal: Boolean = false,
    val isRelay: Boolean = false,
    val isConnected: Boolean = true,
    val hopCount: Int = 1,
    val nextHopId: String? = null
)

@Immutable
data class BatteryImpactUi(
    val batteryLevelText: String = "88%",
    val impactLevel: String = "LOW",
    val isPowerSaveActive: Boolean = false,
    val bleUsagePercent: Int = 60,
    val wifiUsagePercent: Int = 40,
    val cpuUsagePercent: Int = 12,
    val memoryUsageMb: Int = 42,
    val backgroundActivityText: String = "Active (Mesh Relay Engine)"
)

@Immutable
data class StorageStatisticsUi(
    val routeTableEntries: Int = 0,
    val pendingQueueSize: Int = 0,
    val storeAndForwardCount: Int = 0,
    val duplicateCacheSize: Int = 0,
    val activeTransfersCount: Int = 0
)

@Immutable
data class DiagnosticEventUi(
    val id: String,
    val timestamp: Long,
    val title: String,
    val detail: String,
    val category: LogType,
    val nodeId: String? = null
)

enum class TimelineEventType {
    CONNECTED,
    DISCONNECTED,
    FILE_TRANSFER,
    BROADCAST,
    ROUTE_CHANGE,
    SYNC
}

enum class TimelineTimeGroup {
    TODAY,
    YESTERDAY,
    EARLIER
}

@Immutable
data class TimelineEventUi(
    val id: String,
    val timestamp: Long,
    val type: TimelineEventType,
    val title: String,
    val description: String,
    val nodeOrPeer: String? = null,
    val timeGroup: TimelineTimeGroup = TimelineTimeGroup.TODAY
)

enum class AnalyticsFilterType(val label: String) {
    ALL("All"),
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    NETWORK("Network"),
    ROUTING("Routing"),
    TRANSFERS("Transfers")
}

@Immutable
data class ChartDataPoint(
    val xLabel: String,
    val value: Float,
    val secondaryValue: Float? = null
)

@Immutable
data class ChartSeries(
    val title: String,
    val unit: String,
    val points: List<ChartDataPoint>
)

@Immutable
data class LogEntryUi(
    val id: String,
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String,
    val rawLogType: LogType = LogType.RELAY
)

/**
 * Tactical Command Center master UI state wrapping ViewModel inputs safely.
 */
@Immutable
data class TacticalAnalyticsUiState(
    val health: MeshHealthUi = MeshHealthUi(),
    val connectionQuality: ConnectionQualityUi = ConnectionQualityUi(),
    val packetStats: PacketStatisticsUi = PacketStatisticsUi(),
    val transferStats: TransferAnalyticsUi = TransferAnalyticsUi(),
    val batteryImpact: BatteryImpactUi = BatteryImpactUi(),
    val storageStats: StorageStatisticsUi = StorageStatisticsUi(),
    val hopDistribution: Map<Int, Int> = emptyMap(),
    val activeNodes: Set<String> = emptySet(),
    val routes: List<RouteEntry> = emptyList(),
    val events: List<DiagnosticEventUi> = emptyList(),
    val timelineEvents: List<TimelineEventUi> = emptyList(),
    val throughputSeries: ChartSeries = ChartSeries("Throughput", "msg/s", emptyList()),
    val logEntries: List<LogEntryUi> = emptyList(),
    val isExporting: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: AnalyticsFilterType = AnalyticsFilterType.ALL,
    val selectedLogFilter: LogType? = null
)
