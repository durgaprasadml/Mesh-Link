package com.meshlink.ble.data

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

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
    WIFI_NEGOTIATION,
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
    MAP_SYNC
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
    val encrypted: Boolean = false, // True if payload is AES-256-GCM ciphertext
    var ttl: Int = 10,
    var hopCount: Int = 0,
    val visitedPath: MutableList<String> = mutableListOf()
)

object MeshPacketParser {
    fun toJson(packet: MeshPacket): String {
        val json = JSONObject()
        json.put("packetId", packet.packetId)
        json.put("senderId", packet.senderId)
        json.put("targetId", packet.targetId)
        json.put("payload", packet.payload)
        
        if (packet.type != PacketType.TEXT) json.put("type", packet.type.name)
        if (packet.priority != PacketPriority.NORMAL) json.put("priority", packet.priority.name)
        if (packet.broadcastType != BroadcastType.NONE) json.put("broadcastType", packet.broadcastType.name)
        if (packet.encrypted) json.put("encrypted", true)
        if (packet.ttl != 10) json.put("ttl", packet.ttl)
        if (packet.hopCount != 0) json.put("hopCount", packet.hopCount)

        packet.transferId?.let { json.put("transferId", it) }
        if (packet.type != PacketType.TEXT) {
            json.put("chunkIndex", packet.chunkIndex)
            json.put("totalChunks", packet.totalChunks)
        }
        packet.mimeType?.let { json.put("mimeType", it) }

        if (packet.visitedPath.isNotEmpty()) {
            val array = JSONArray()
            packet.visitedPath.forEach { array.put(it) }
            json.put("visitedPath", array)
        }

        return json.toString()
    }

    fun fromJson(jsonString: String): MeshPacket? {
        return try {
            val json = JSONObject(jsonString)
            val path = mutableListOf<String>()
            val pathArray = json.optJSONArray("visitedPath")
            if (pathArray != null) {
                for (i in 0 until pathArray.length()) {
                    path.add(pathArray.getString(i))
                }
            }
            val typeName = json.optString("type", "TEXT")
            val packetType = try { PacketType.valueOf(typeName) } catch (_: Exception) { PacketType.TEXT }
            
            val priorityName = json.optString("priority", "NORMAL")
            val packetPriority = try { PacketPriority.valueOf(priorityName) } catch (_: Exception) { PacketPriority.NORMAL }
            
            val broadcastName = json.optString("broadcastType", "NONE")
            val broadcastType = try { BroadcastType.valueOf(broadcastName) } catch (_: Exception) { BroadcastType.NONE }

            MeshPacket(
                packetId = json.getString("packetId"),
                senderId = json.getString("senderId"),
                targetId = json.getString("targetId"),
                payload = json.getString("payload"),
                type = packetType,
                priority = packetPriority,
                broadcastType = broadcastType,
                transferId = json.optString("transferId", null),
                chunkIndex = json.optInt("chunkIndex", 0),
                totalChunks = json.optInt("totalChunks", 0),
                mimeType = json.optString("mimeType", null),
                encrypted = json.optBoolean("encrypted", false),
                ttl = json.optInt("ttl", 10),
                hopCount = json.optInt("hopCount", 0),
                visitedPath = path
            )
        } catch (e: Exception) {
            null
        }
    }
}
