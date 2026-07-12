package com.meshlink.data.ble

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class PacketType {
    TEXT,
    MEDIA_META,
    MEDIA_CHUNK,
    MEDIA_ACK,   // Receiver acknowledges a chunk was received
    MEDIA_NACK,  // Receiver requests resend of missing chunks
    LOCATION,
    SOS,
    KEY_EXCHANGE,
    DELIVERY_ACK,
    READ_RECEIPT
}

data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val targetId: String,
    val payload: String,
    val type: PacketType = PacketType.TEXT,
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
        json.put("type", packet.type.name)
        json.put("encrypted", packet.encrypted)
        json.put("ttl", packet.ttl)
        json.put("hopCount", packet.hopCount)

        packet.transferId?.let { json.put("transferId", it) }
        if (packet.type != PacketType.TEXT) {
            json.put("chunkIndex", packet.chunkIndex)
            json.put("totalChunks", packet.totalChunks)
        }
        packet.mimeType?.let { json.put("mimeType", it) }

        val array = JSONArray()
        packet.visitedPath.forEach { array.put(it) }
        json.put("visitedPath", array)

        return json.toString()
    }

    fun fromJson(jsonString: String): MeshPacket? {
        return try {
            val json = JSONObject(jsonString)
            val pathArray = json.getJSONArray("visitedPath")
            val path = mutableListOf<String>()
            for (i in 0 until pathArray.length()) {
                path.add(pathArray.getString(i))
            }
            val typeName = json.optString("type", "TEXT")
            val packetType = try { PacketType.valueOf(typeName) } catch (_: Exception) { PacketType.TEXT }

            MeshPacket(
                packetId = json.getString("packetId"),
                senderId = json.getString("senderId"),
                targetId = json.getString("targetId"),
                payload = json.getString("payload"),
                type = packetType,
                transferId = json.optString("transferId", null),
                chunkIndex = json.optInt("chunkIndex", 0),
                totalChunks = json.optInt("totalChunks", 0),
                mimeType = json.optString("mimeType", null),
                encrypted = json.optBoolean("encrypted", false),
                ttl = json.getInt("ttl"),
                hopCount = json.getInt("hopCount"),
                visitedPath = path
            )
        } catch (e: Exception) {
            null
        }
    }
}
