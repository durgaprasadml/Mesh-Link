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

@Singleton
class BroadcastHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val locationProvider: LocationProvider,
    private val router: Router,
    private val packetDispatcher: PacketDispatcher
) {
    private val TAG = "BroadcastHandler"

    suspend fun sendSos() {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        router.localMeshId = localPeerId

        val location = locationProvider.getCurrentLocation()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val battery = location?.batteryPercent ?: locationProvider.getBatteryPercent()

        val payloadJson = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            put("battery", battery)
            put("timestamp", System.currentTimeMillis())
            put("senderName", user.name)
        }.toString()

        val packet = MeshPacket(
            senderId = localPeerId,
            targetId = "BROADCAST",
            payload = payloadJson,
            type = PacketType.SOS,
            encrypted = false,
            ttl = 15
        )
        packetDispatcher.dispatchSinglePacket("BROADCAST", packet)
    }

    suspend fun receiveSosMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) return

        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val lat = json.optDouble("lat", 0.0)
        val lng = json.optDouble("lng", 0.0)
        val battery = json.optInt("battery", -1)
        val senderName = json.optString("senderName", MeshIdNormalizer.canonicalize(packet.senderId))

        val chatId = MeshIdNormalizer.canonicalize(packet.senderId)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            text = "🚨 SOS EMERGENCY from $senderName — Lat: $lat, Lng: $lng — Battery: $battery%",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, "🚨 $senderName")
    }

    suspend fun broadcastMessage(messageText: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        router.localMeshId = localPeerId

        val payloadJson = JSONObject().apply {
            put("text", "[BROADCAST] $messageText")
            put("senderName", user.name)
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
        packetDispatcher.dispatchSinglePacket("BROADCAST", packet)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = localPeerId,
            text = "[BROADCAST] $messageText",
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

        val (plaintext, senderName) = try {
            val json = JSONObject(rawPayload)
            if (json.has("text")) {
                json.getString("text") to json.optString("senderName", MeshIdNormalizer.canonicalize(packet.senderId))
            } else {
                MeshLogger.w(TAG, "Filtering out JSON protocol packet masquerading as broadcast text: $rawPayload")
                return
            }
        } catch (_: Exception) {
            rawPayload to MeshIdNormalizer.canonicalize(packet.senderId)
        }

        val trimmedPlaintext = plaintext.trim()
        if (trimmedPlaintext.startsWith("v2|") || internalKeywords.contains(trimmedPlaintext)) {
            MeshLogger.w(TAG, "Filtering out internal protocol packet from broadcast UI (after JSON extraction): $plaintext")
            return
        }

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = "BROADCAST",
            senderId = packet.senderId,
            text = plaintext,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
        )
        chatDao.insertMessage(message)
        NotificationHelper.showMessageNotification(context, packet.senderId, "📢 $senderName", plaintext)
    }
}
