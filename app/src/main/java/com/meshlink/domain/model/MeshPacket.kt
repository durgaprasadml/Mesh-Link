package com.meshlink.domain.model

import java.util.UUID

enum class PacketType {
    TEXT,
    MEDIA_META,
    MEDIA_CHUNK,
    MEDIA_ACK,
    MEDIA_NACK,
    LOCATION,
    SOS,
    KEY_EXCHANGE,
    DELIVERY_ACK,
    READ_RECEIPT,
    SESSION_REKEY,
    VOICE_SIGNAL,
    VOICE_FRAME,
    VIDEO_SIGNAL,
    VIDEO_FRAME,
    BEACON,
    INCIDENT_REPORT,
    CHECK_IN,
    FORM_SYNC,
    RESOURCE_SYNC,
    MAP_SYNC,
    PROFILE_IMAGE_REQUEST,
    PROFILE_IMAGE_RESPONSE
}

enum class PacketPriority(val level: Int) {
    CRITICAL(4),
    HIGH(3),
    NORMAL(2),
    LOW(1),
    BACKGROUND(0)
}

enum class BroadcastType {
    NONE,
    GLOBAL,
    REGIONAL,
    LOCAL,
    COMMAND,
    MEDICAL,
    SOS
}

data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val targetId: String, // Can be "BROADCAST"
    val payload: String,
    val type: PacketType = PacketType.TEXT,
    val priority: PacketPriority = PacketPriority.NORMAL,
    val broadcastType: BroadcastType = BroadcastType.NONE,
    val transferId: String? = null,
    val chunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val mimeType: String? = null,
    val encrypted: Boolean = false,
    val ttl: Int = 10,
    val hopCount: Int = 0,
    val visitedPath: List<String> = emptyList(),
    val sequenceNumber: Long = 0L,
    val sourceSequenceNumber: Long = 0L,
    val nextHop: String? = null,
    val previousHop: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val transport: RouteType = RouteType.BLE,
    val flags: Int = 0
)
