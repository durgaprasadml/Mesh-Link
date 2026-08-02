package com.meshlink.ui.chat

import androidx.compose.runtime.Immutable
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType

/**
 * Presentation-only models for the Mesh-Link Phase 5 Chat UI Redesign.
 * None of these classes replace or modify domain models.
 */

enum class BubblePosition {
    SINGLE, FIRST, MIDDLE, LAST
}

enum class DeliveryState {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    RELAYED,
    FAILED;

    companion object {
        fun fromDomain(status: DeliveryStatus): DeliveryState {
            return when (status) {
                DeliveryStatus.PENDING, DeliveryStatus.QUEUED -> PENDING
                DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ROUTE -> SENDING
                DeliveryStatus.SENT, DeliveryStatus.WAITING_FOR_ACK -> SENT
                DeliveryStatus.DELIVERED -> DELIVERED
                DeliveryStatus.SEEN -> READ
                DeliveryStatus.RELAYED -> RELAYED
                DeliveryStatus.EXPIRED, DeliveryStatus.CANCELLED, DeliveryStatus.PERMANENT_FAILURE, DeliveryStatus.FAILED -> FAILED
            }
        }
    }
}

@Immutable
data class MessageReaction(
    val emoji: String,
    val senderId: String,
    val count: Int = 1
)

@Immutable
data class ReplyState(
    val targetMessageId: String,
    val senderName: String,
    val messageSnippet: String,
    val messageType: MessageType = MessageType.TEXT
)

@Immutable
data class ChatBubbleUiState(
    val message: Message,
    val position: BubblePosition = BubblePosition.SINGLE,
    val isSelected: Boolean = false,
    val isSelectionMode: Boolean = false,
    val reactions: List<MessageReaction> = emptyList(),
    val replyState: ReplyState? = null,
    val isForwarded: Boolean = false,
    val isEdited: Boolean = false,
    val deliveryState: DeliveryState = DeliveryState.fromDomain(message.status),
    val currentlyPlayingVoiceId: String? = null,
    val playbackProgress: Float = 0f,
    val transferProgress: Float? = null
)

@Immutable
data class MessageGroup(
    val groupId: String,
    val dateLabel: String,
    val timestamp: Long,
    val messages: List<Message>
)

@Immutable
data class TypingState(
    val isTyping: Boolean = false,
    val peerName: String = "",
    val isViaRelay: Boolean = false,
    val nodeCount: Int = 1
)

@Immutable
data class SelectionState(
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

enum class AttachmentType {
    GALLERY, CAMERA, VOICE, LOCATION, DOCUMENT, CONTACT
}

@Immutable
data class AttachmentState(
    val type: AttachmentType,
    val label: String,
    val iconRes: Int? = null
)
