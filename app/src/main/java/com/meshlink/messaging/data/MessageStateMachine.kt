package com.meshlink.messaging.data

import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageStateMachine @Inject constructor(
    private val chatDao: ChatDao
) {
    suspend fun transitionToQueued(messageId: String) {
        chatDao.updateMessageStatus(messageId, DeliveryStatus.QUEUED)
    }

    suspend fun transitionToSent(messageId: String) {
        // Can add validation here: e.g., ensure it's not already DELIVERED
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (current != DeliveryStatus.DELIVERED && current != DeliveryStatus.SEEN) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
        }
    }

    suspend fun transitionToDelivered(messageId: String) {
        chatDao.updateMessageStatus(messageId, DeliveryStatus.DELIVERED)
    }

    suspend fun transitionToSeen(messageId: String) {
        chatDao.updateMessageStatus(messageId, DeliveryStatus.SEEN)
    }

    suspend fun transitionToFailed(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (current != DeliveryStatus.DELIVERED && current != DeliveryStatus.SEEN) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.FAILED)
        }
    }
}
