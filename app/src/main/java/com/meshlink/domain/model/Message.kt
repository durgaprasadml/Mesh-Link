package com.meshlink.domain.model



data class Message(
    val messageId: String,
    val chatId: String,
    val text: String,
    val senderId: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: DeliveryStatus,
    val messageType: MessageType,
    val mediaPath: String? = null,
    val mediaDurationMs: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercent: Int? = null
)

enum class DeliveryStatus {
    PENDING, SENT, RELAYED, DELIVERED, SEEN, FAILED
}

enum class MessageType {
    TEXT, IMAGE, VOICE, LOCATION, SOS, DOCUMENT
}
