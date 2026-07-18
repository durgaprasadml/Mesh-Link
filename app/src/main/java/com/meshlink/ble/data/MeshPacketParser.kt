package com.meshlink.ble.data

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.BroadcastType
import org.json.JSONArray
import org.json.JSONObject

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
                transferId = if (json.has("transferId")) json.getString("transferId") else null,
                chunkIndex = json.optInt("chunkIndex", 0),
                totalChunks = json.optInt("totalChunks", 0),
                mimeType = if (json.has("mimeType")) json.getString("mimeType") else null,
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
