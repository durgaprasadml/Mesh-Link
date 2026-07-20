package com.meshlink.database.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageType {
    TEXT, IMAGE, VOICE, LOCATION, SOS, DOCUMENT
}

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["messageId"], unique = true),
        Index(value = ["chatId", "timestamp"]),
        Index(value = ["status"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val messageId: String, // Unique UUID for message
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: DeliveryStatus,
    val messageType: MessageType = MessageType.TEXT,
    val mediaPath: String? = null,
    val mediaDurationMs: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercent: Int? = null
)

enum class DeliveryStatus {
    PENDING, SENT, RELAYED, DELIVERED, SEEN, FAILED
}
