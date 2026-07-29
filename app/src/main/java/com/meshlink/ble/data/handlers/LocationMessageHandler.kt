package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MeshIdNormalizer
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationMessageHandler @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val locationProvider: LocationProvider,
    private val packetDispatcher: PacketDispatcher
) {
    suspend fun sendLocation(targetMeshId: String, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        val targetPeerId = MeshIdNormalizer.canonicalize(targetMeshId)

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

        val generatedMessageId = UUID.randomUUID().toString()
        val packet = MeshPacket(
            packetId = generatedMessageId,
            senderId = localPeerId,
            targetId = targetPeerId,
            payload = payloadJson,
            type = PacketType.LOCATION,
            encrypted = false
        )
        val result = packetDispatcher.dispatchSinglePacket(targetPeerId, packet)
        
        val initialStatus = when (result) {
            is com.meshlink.domain.model.DispatchResult.Queued -> DeliveryStatus.QUEUED
            else -> DeliveryStatus.WAITING_FOR_ROUTE
        }

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = targetPeerId,
            senderId = localPeerId,
            text = "📍 Location: $lat, $lng",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = initialStatus,
            messageType = MessageType.LOCATION,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)
    }

    suspend fun receiveLocationMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) {
            userRepository.getLocalUser()?.let { user ->
                val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
                val ackPacket = MeshPacket(
                    senderId = localPeerId,
                    targetId = packet.senderId,
                    payload = packet.packetId,
                    type = PacketType.DELIVERY_ACK,
                    encrypted = false
                )
                packetDispatcher.dispatchSinglePacket(packet.senderId, ackPacket)
            }
            return
        }

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
            text = "📍 Location: $lat, $lng",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.LOCATION,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
        
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = packet.senderId,
                payload = packet.packetId,
                type = PacketType.DELIVERY_ACK,
                encrypted = false
            )
            packetDispatcher.dispatchSinglePacket(packet.senderId, ackPacket)
        }
    }
}
