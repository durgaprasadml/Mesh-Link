package com.meshlink.messaging.data

import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageStateMachine @Inject constructor(
    private val chatDao: ChatDao
) {
    private fun isTerminalState(status: DeliveryStatus?): Boolean {
        return when (status) {
            DeliveryStatus.DELIVERED,
            DeliveryStatus.SEEN,
            DeliveryStatus.EXPIRED,
            DeliveryStatus.CANCELLED,
            DeliveryStatus.PERMANENT_FAILURE -> true
            else -> false
        }
    }

    suspend fun transitionToPending(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.PENDING)
        }
    }

    suspend fun transitionToQueued(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.QUEUED)
        }
    }

    suspend fun transitionToSending(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.SENDING)
        }
    }

    suspend fun transitionToSent(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.SENT)
        }
    }

    suspend fun transitionToWaitingForAck(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.WAITING_FOR_ACK)
        }
    }

    suspend fun transitionToRetrying(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.RETRYING)
        }
    }

    suspend fun transitionToWaitingForRoute(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.WAITING_FOR_ROUTE)
        }
    }

    suspend fun transitionToDelivered(messageId: String) {
        chatDao.updateMessageStatus(messageId, DeliveryStatus.DELIVERED)
    }

    suspend fun transitionToSeen(messageId: String) {
        chatDao.updateMessageStatus(messageId, DeliveryStatus.SEEN)
    }

    suspend fun transitionToExpired(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.EXPIRED)
        }
    }

    suspend fun transitionToCancelled(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.CANCELLED)
        }
    }

    suspend fun transitionToPermanentFailure(messageId: String) {
        val current = chatDao.getMessageByUuid(messageId)?.status
        if (!isTerminalState(current)) {
            chatDao.updateMessageStatus(messageId, DeliveryStatus.PERMANENT_FAILURE)
        }
    }

    suspend fun transitionToFailed(messageId: String) {
        // Backward compatibility mapping: map explicit failure calls to PERMANENT_FAILURE
        transitionToPermanentFailure(messageId)
    }
}
