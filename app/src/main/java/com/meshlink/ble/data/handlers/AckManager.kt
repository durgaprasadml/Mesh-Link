package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MeshIdNormalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AckManager @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val packetDispatcher: PacketDispatcher
) {
    suspend fun handleDeliveryAck(packet: MeshPacket) {
        chatDao.updateMessageStatus(packet.payload, DeliveryStatus.DELIVERED)
    }

    suspend fun handleReadReceipt(packet: MeshPacket) {
        chatDao.updateMessageStatus(packet.payload, DeliveryStatus.SEEN)
    }

    suspend fun sendReadReceipts(chatId: String) {
        val unreadIds = chatDao.getUnreadIncomingMessages(chatId)
        if (unreadIds.isEmpty()) return

        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)

        // Mark as seen locally
        chatDao.markMessagesAsSeen(unreadIds)

        // Send READ_RECEIPT packets
        // The chatId is the target meshId (for direct chats)
        val targetPeerId = MeshIdNormalizer.canonicalize(chatId)
        if (targetPeerId == "BROADCAST") return // No read receipts for broadcasts

        unreadIds.forEach { msgId ->
            val receiptPacket = MeshPacket(
                senderId = localPeerId,
                targetId = targetPeerId,
                payload = msgId, // The ID of the message being marked as seen
                type = PacketType.READ_RECEIPT,
                encrypted = false
            )
            packetDispatcher.dispatchSinglePacket(targetPeerId, receiptPacket)
        }
    }
}
