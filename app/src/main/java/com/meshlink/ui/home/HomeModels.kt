package com.meshlink.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tactical Activity Event representing live network activity on the home timeline.
 */
@Immutable
data class MeshActivityEvent(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val icon: ImageVector,
    val type: EventType
) {
    enum class EventType {
        PEER_JOINED,
        PEER_LEFT,
        BROADCAST_RECEIVED,
        SOS_ALERT,
        ROUTE_DISCOVERED,
        HANDSHAKE_COMPLETED
    }
}

/**
 * Telemetry summary state for Mesh-Link tactical network overview.
 */
@Immutable
data class MeshTelemetryState(
    val activeNodeCount: Int = 0,
    val signalQualityPercent: Int = 0,
    val activeRoutes: Int = 0,
    val bleEnabled: Boolean = true,
    val wifiDirectEnabled: Boolean = true,
    val encryptionActive: Boolean = true,
    val batteryEfficiencyPercent: Int = 98
)

/**
 * Contextual recommendation card model for dynamic alerts on the home screen.
 */
@Immutable
data class MeshRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val actionText: String,
    val icon: ImageVector,
    val priority: RecommendationPriority,
    val onClick: () -> Unit
) {
    enum class RecommendationPriority {
        URGENT,
        HIGH,
        NORMAL,
        SUGGESTION
    }
}
