package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.messaging.data.DeliveryTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AckManager @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val packetDispatcher: PacketDispatcher,
    private val deliveryTracker: DeliveryTracker,
    private val retryCoordinator: com.meshlink.common.recovery.RetryCoordinator
) {
    companion object {
        const val MAX_BATCH_SIZE = 10
    }

    suspend fun handleDeliveryAck(packet: MeshPacket) {
        val packetId = packet.payload
        retryCoordinator.cancelRetryForPacket(packetId)
        deliveryTracker.onAckReceived(packetId)
    }

    suspend fun handleReadReceipt(packet: MeshPacket) {
        val payload = packet.payload.trim()
        if (payload.isBlank()) return

        val ids = payload.split(",")
        ids.forEach { id ->
            val cleanId = id.trim()
            if (cleanId.isNotBlank()) {
                retryCoordinator.cancelRetryForPacket(cleanId)
                deliveryTracker.onReadReceiptReceived(cleanId)
            }
        }
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

        unreadIds.chunked(MAX_BATCH_SIZE).forEach { batch ->
            val batchPayload = batch.joinToString(",")
            val receiptPacket = MeshPacket(
                senderId = localPeerId,
                targetId = targetPeerId,
                payload = batchPayload,
                type = PacketType.READ_RECEIPT,
                encrypted = false
            )
            packetDispatcher.dispatchSinglePacket(targetPeerId, receiptPacket)
        }
    }
}
