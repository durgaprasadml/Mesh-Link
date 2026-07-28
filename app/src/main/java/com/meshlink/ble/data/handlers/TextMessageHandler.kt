package com.meshlink.ble.data.handlers

import android.content.Context
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextMessageHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val packetDispatcher: PacketDispatcher
) {
    private val TAG = "TextMessageHandler"

    suspend fun sendMessage(targetMeshId: String, message: Message) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        val targetPeerId = MeshIdNormalizer.canonicalize(targetMeshId)

        val messageId = message.messageId

        val wrappedPayload = JSONObject().apply {
            put("text", message.text)
            put("senderName", user.name)
        }.toString()

        val packet = MeshPacket(
            packetId = messageId,
            senderId = localPeerId,
            targetId = targetPeerId,
            payload = wrappedPayload,
            type = PacketType.TEXT,
            encrypted = false
        )
        if (packetDispatcher.dispatchSinglePacket(targetPeerId, packet)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
        }
    }

    suspend fun receiveMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) {
            // Already processed this message! Just send ACK in case it was lost.
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

        val chatId = MeshIdNormalizer.canonicalize(packet.senderId)

        val rawPayload = packet.payload
        val internalKeywords = setOf("KEY_EXCHANGE", "ACK", "RELAY", "ROUTING", "HANDSHAKE")

        val (plaintext, senderName) = try {
            val json = JSONObject(rawPayload)
            if (json.has("text")) {
                json.getString("text") to json.optString("senderName", MeshIdNormalizer.canonicalize(packet.senderId))
            } else {
                MeshLogger.w(TAG, "Filtering out JSON protocol packet masquerading as text: $rawPayload")
                return
            }
        } catch (_: Exception) {
            rawPayload to MeshIdNormalizer.canonicalize(packet.senderId)
        }

        val trimmedPlaintext = plaintext.trim()
        if (trimmedPlaintext.startsWith("v2|") || internalKeywords.contains(trimmedPlaintext)) {
            MeshLogger.w(TAG, "Filtering out internal protocol packet from chat UI (after JSON extraction): $plaintext")
            return
        }

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            text = plaintext,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.TEXT
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

        NotificationHelper.showMessageNotification(context, packet.senderId, senderName, plaintext)
    }
}
