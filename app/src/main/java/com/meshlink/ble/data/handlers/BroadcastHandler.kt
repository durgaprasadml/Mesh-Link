package com.meshlink.ble.data.handlers

import android.content.Context
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

import com.meshlink.database.data.local.UserDao
import com.meshlink.database.data.local.UserEntity

@Singleton
class BroadcastHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val locationProvider: LocationProvider,
    private val router: Router
) {
    private val TAG = "BroadcastHandler"

    suspend fun sendSos(sosEventId: String = java.util.UUID.randomUUID().toString()) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        router.localMeshId = localPeerId

        val location = locationProvider.getCurrentLocation()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val battery = location?.batteryPercent ?: locationProvider.getBatteryPercent()

        val displayName = user.name.trim().ifBlank { "Unknown User" }

        val payloadJson = JSONObject().apply {
            put("sosEventId", sosEventId)
            put("lat", lat)
            put("lng", lng)
            put("battery", battery)
            put("timestamp", System.currentTimeMillis())
            put("senderName", displayName)
        }.toString()

        val packet = MeshPacket(
            packetId = sosEventId,
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.SOS,
            priority = com.meshlink.domain.model.PacketPriority.CRITICAL,
            broadcastType = com.meshlink.domain.model.BroadcastType.SOS,
            encrypted = false,
            ttl = 15
        )
        router.routeMediaPacket(packet)
    }

    suspend fun receiveSosMessage(packet: MeshPacket) {
        val json = try { JSONObject(packet.payload) } catch (_: Exception) { null }
        val sosEventId = json?.optString("sosEventId", packet.packetId)?.ifBlank { packet.packetId } ?: packet.packetId

        val existing = chatDao.getMessageByUuid(sosEventId)
        val lat = json?.optDouble("lat", 0.0) ?: 0.0
        val lng = json?.optDouble("lng", 0.0) ?: 0.0
        val battery = json?.optInt("battery", -1) ?: -1
        val payloadSenderName = json?.optString("senderName", "")?.trim() ?: ""

        val existingUser = userDao.getUser(packet.senderId)
        val resolvedSenderName = when {
            existingUser != null && existingUser.name.isNotBlank() && existingUser.name != "Unknown User" -> existingUser.name
            payloadSenderName.isNotBlank() -> payloadSenderName
            else -> "Unknown User"
        }

        if (resolvedSenderName != "Unknown User") {
            if (existingUser == null) {
                userDao.insertUser(UserEntity(meshId = packet.senderId, name = resolvedSenderName))
            } else if (existingUser.name.isBlank() || existingUser.name == "Unknown User" || existingUser.name == packet.senderId) {
                userDao.insertUser(existingUser.copy(name = resolvedSenderName))
            }
        }

        val chatId = MeshIdNormalizer.canonicalize(packet.senderId)

        if (existing != null) {
            // If already exists (e.g. placeholder from media), update details while preserving existing mediaPath
            val updatedMessage = existing.copy(
                text = "🚨 SOS EMERGENCY from $resolvedSenderName — Lat: $lat, Lng: $lng — Battery: $battery%",
                latitude = lat,
                longitude = lng,
                batteryPercent = battery,
                status = DeliveryStatus.DELIVERED
            )
            chatDao.insertMessageAndUpdateChat(updatedMessage, "🚨 $resolvedSenderName")
            return
        }

        val message = MessageEntity(
            messageId = sosEventId,
            chatId = chatId,
            senderId = packet.senderId,
            text = "🚨 SOS EMERGENCY from $resolvedSenderName — Lat: $lat, Lng: $lng — Battery: $battery%",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, "🚨 $resolvedSenderName")
    }


    suspend fun broadcastMessage(messageText: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        router.localMeshId = localPeerId

        val displayName = user.name.trim().ifBlank { "Unknown User" }
        val cleanText = messageText.trim()

        val payloadJson = JSONObject().apply {
            put("text", cleanText)
            put("senderName", displayName)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val packet = MeshPacket(
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.TEXT,
            encrypted = false,
            ttl = 15
        )
        router.routeMediaPacket(packet)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = localPeerId,
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
    }

    suspend fun receiveBroadcastTextMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) return

        val rawPayload = packet.payload
        val internalKeywords = setOf("KEY_EXCHANGE", "ACK", "RELAY", "ROUTING", "HANDSHAKE")

        val (rawText, payloadSenderName) = try {
            val json = JSONObject(rawPayload)
            if (json.has("text")) {
                json.getString("text") to json.optString("senderName", "").trim()
            } else {
                MeshLogger.w(TAG, "Filtering out JSON protocol packet masquerading as broadcast text: $rawPayload")
                return
            }
        } catch (_: Exception) {
            rawPayload to ""
        }

        val cleanText = if (rawText.startsWith("[BROADCAST]")) {
            rawText.removePrefix("[BROADCAST]").trim()
        } else {
            rawText.trim()
        }

        if (cleanText.startsWith("v2|") || internalKeywords.contains(cleanText)) {
            MeshLogger.w(TAG, "Filtering out internal protocol packet from broadcast UI (after JSON extraction): $cleanText")
            return
        }

        val existingUser = userDao.getUser(packet.senderId)
        val canonicalSenderId = MeshIdNormalizer.canonicalize(packet.senderId)

        val resolvedSenderName = when {
            existingUser != null && existingUser.name.isNotBlank() && existingUser.name != "Unknown User" -> existingUser.name
            payloadSenderName.isNotBlank() && payloadSenderName != canonicalSenderId -> payloadSenderName
            else -> "Unknown User"
        }

        if (resolvedSenderName != "Unknown User") {
            if (existingUser == null) {
                userDao.insertUser(UserEntity(meshId = packet.senderId, name = resolvedSenderName))
            } else if (existingUser.name.isBlank() || existingUser.name == "Unknown User" || existingUser.name == canonicalSenderId) {
                userDao.insertUser(existingUser.copy(name = resolvedSenderName))
            }
        }

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = packet.senderId,
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
        NotificationHelper.showMessageNotification(context, packet.senderId, "📢 Broadcast from $resolvedSenderName", cleanText)
    }
}
