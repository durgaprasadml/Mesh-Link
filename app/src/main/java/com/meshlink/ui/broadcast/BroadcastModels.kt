package com.meshlink.ui.broadcast

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.UserIdentity

/**
 * Tactical Broadcast Priority Tier.
 */
enum class BroadcastPriority(
    val label: String,
    val badgeColor: Long,
    val containerColor: Long,
    val isEmergency: Boolean = false
) {
    NORMAL("Normal", 0xFF0284C7, 0x1A0284C7),
    HIGH("High Priority", 0xFFF59E0B, 0x22F59E0B),
    URGENT("Urgent", 0xFFEF4444, 0x2AEF4444),
    EMERGENCY("SOS Emergency", 0xFFFF0055, 0x33FF0055, isEmergency = true);

    companion object {
        fun fromMessage(message: Message): BroadcastPriority {
            return when {
                message.messageType == MessageType.SOS -> EMERGENCY
                message.text.contains("[EMERGENCY]", ignoreCase = true) || 
                message.text.contains("SOS", ignoreCase = true) -> EMERGENCY
                message.text.contains("[URGENT]", ignoreCase = true) -> URGENT
                message.text.contains("[HIGH]", ignoreCase = true) -> HIGH
                else -> NORMAL
            }
        }
    }
}

/**
 * Presentation-layer Delivery State classification.
 */
enum class BroadcastDeliveryState(
    val label: String,
    val colorHex: Long
) {
    PENDING("Queued", 0xFFF59E0B),
    SENDING("Broadcasting...", 0xFF00E5FF),
    SENT("Sent to Mesh", 0xFF0284C7),
    DELIVERED("Delivered", 0xFF00F59B),
    FAILED("Delivery Failed", 0xFFFF0055);

    companion object {
        fun fromDomain(status: DeliveryStatus): BroadcastDeliveryState = when (status) {
            DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.WAITING_FOR_ROUTE -> PENDING
            DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK -> SENDING
            DeliveryStatus.SENT, DeliveryStatus.RELAYED -> SENT
            DeliveryStatus.DELIVERED, DeliveryStatus.SEEN -> DELIVERED
            DeliveryStatus.EXPIRED, DeliveryStatus.CANCELLED, DeliveryStatus.PERMANENT_FAILURE, DeliveryStatus.FAILED -> FAILED
        }
    }
}

/**
 * Presentation-only UI wrapper for domain Message.
 */
@Immutable
data class BroadcastMessageUiState(
    val message: Message,
    val senderIdentity: UserIdentity?,
    val priority: BroadcastPriority = BroadcastPriority.fromMessage(message),
    val deliveryState: BroadcastDeliveryState = BroadcastDeliveryState.fromDomain(message.status),
    val isExpanded: Boolean = false
) {
    val isMe: Boolean get() = message.isFromMe
    val messageId: String get() = message.messageId
    val text: String get() = message.text
    val timestamp: Long get() = message.timestamp
    val hasLocation: Boolean get() = message.latitude != null && message.longitude != null
    val hasAttachment: Boolean get() = !message.mediaPath.isNullOrBlank()
}

/**
 * Presentation filter criteria state.
 */
@Immutable
data class BroadcastFilterState(
    val searchQuery: String = "",
    val selectedPriority: BroadcastPriority? = null,
    val selectedDeliveryState: BroadcastDeliveryState? = null,
    val emergencyOnly: Boolean = false,
    val filterMeOnly: Boolean = false,
    val filterPeersOnly: Boolean = false
) {
    val isActive: Boolean
        get() = searchQuery.isNotBlank() || selectedPriority != null || 
                selectedDeliveryState != null || emergencyOnly || filterMeOnly || filterPeersOnly

    fun matches(uiState: BroadcastMessageUiState): Boolean {
        if (emergencyOnly && uiState.priority != BroadcastPriority.EMERGENCY) return false
        if (selectedPriority != null && uiState.priority != selectedPriority) return false
        if (selectedDeliveryState != null && uiState.deliveryState != selectedDeliveryState) return false
        if (filterMeOnly && !uiState.isMe) return false
        if (filterPeersOnly && uiState.isMe) return false
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim()
            val matchesText = uiState.text.contains(query, ignoreCase = true)
            val matchesSender = uiState.senderIdentity?.displayName?.contains(query, ignoreCase = true) == true ||
                    uiState.message.senderId.contains(query, ignoreCase = true)
            if (!matchesText && !matchesSender) return false
        }
        return true
    }
}

/**
 * Dashboard Statistics derived strictly from active message list.
 */
@Immutable
data class BroadcastStatisticsUi(
    val totalBroadcasts: Int = 0,
    val deliveredCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val emergencyCount: Int = 0,
    val connectedPeerCount: Int = 0
) {
    val successRatePercentage: Int
        get() = if (totalBroadcasts > 0) ((deliveredCount.toFloat() / totalBroadcasts.toFloat()) * 100).toInt() else 100

    companion object {
        fun calculate(messages: List<Message>, peerCount: Int): BroadcastStatisticsUi {
            var delivered = 0
            var pending = 0
            var failed = 0
            var emergency = 0

            messages.forEach { msg ->
                val state = BroadcastDeliveryState.fromDomain(msg.status)
                when (state) {
                    BroadcastDeliveryState.DELIVERED, BroadcastDeliveryState.SENT -> delivered++
                    BroadcastDeliveryState.PENDING, BroadcastDeliveryState.SENDING -> pending++
                    BroadcastDeliveryState.FAILED -> failed++
                }
                if (BroadcastPriority.fromMessage(msg) == BroadcastPriority.EMERGENCY) {
                    emergency++
                }
            }

            return BroadcastStatisticsUi(
                totalBroadcasts = messages.size,
                deliveredCount = delivered,
                pendingCount = pending,
                failedCount = failed,
                emergencyCount = emergency,
                connectedPeerCount = peerCount
            )
        }
    }
}

/**
 * Presentation recipient UI summary.
 */
@Immutable
data class BroadcastRecipientUi(
    val userId: String,
    val name: String,
    val identity: UserIdentity? = null,
    val isDirectPeer: Boolean = true
)
