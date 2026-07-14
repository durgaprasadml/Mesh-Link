package com.meshlink.database.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relay_packets",
    indices = [
        Index(value = ["targetId"]),
        Index(value = ["expiryTimestamp"])
    ]
)
data class RelayPacketEntity(
    @PrimaryKey val packetId: String,
    val senderId: String,
    val targetId: String,
    val payload: String,
    val type: String,
    val priority: String = "NORMAL",
    val broadcastType: String = "NONE",
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24h TTL
    val ttl: Int,
    val hopCount: Int,
    val encrypted: Boolean,
    val transferId: String? = null,
    val chunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val mimeType: String? = null
)
