package com.meshlink.messaging.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageStateMachine @Inject constructor(
    private val chatDao: ChatDao
) {
    companion object {
        private const val TAG = "MessageStateMachine"
    }

    private fun getStatusPriority(status: DeliveryStatus): Int {
        return when (status) {
            DeliveryStatus.PENDING -> 1
            DeliveryStatus.QUEUED -> 2
            DeliveryStatus.RETRYING -> 3
            DeliveryStatus.WAITING_FOR_ROUTE -> 3
            DeliveryStatus.SENDING -> 4
            DeliveryStatus.WAITING_FOR_ACK -> 5
            DeliveryStatus.SENT -> 6
            DeliveryStatus.DELIVERED -> 7
            DeliveryStatus.RELAYED -> 7
            DeliveryStatus.SEEN -> 8
            DeliveryStatus.EXPIRED -> 9
            DeliveryStatus.CANCELLED -> 9
            DeliveryStatus.FAILED -> 9
            DeliveryStatus.PERMANENT_FAILURE -> 9
        }
    }

    fun isTerminalState(status: DeliveryStatus?): Boolean {
        return when (status) {
            DeliveryStatus.DELIVERED,
            DeliveryStatus.RELAYED,
            DeliveryStatus.SEEN,
            DeliveryStatus.EXPIRED,
            DeliveryStatus.CANCELLED,
            DeliveryStatus.PERMANENT_FAILURE -> true
            else -> false
        }
    }

    private suspend fun transitionTo(
        messageId: String,
        targetStatus: DeliveryStatus,
        allowedPreviousStatuses: List<DeliveryStatus>,
        force: Boolean = false
    ): Boolean {
        val currentMsg = chatDao.getMessageByUuid(messageId)
        val currentStatus = currentMsg?.status

        if (currentStatus == targetStatus) {
            MeshLogger.d(TAG, "Message $messageId is already in target state $targetStatus. Ignoring duplicate transition.")
            return true
        }

        if (currentStatus != null && !force) {
            if (isTerminalState(currentStatus) && targetStatus != DeliveryStatus.SEEN && targetStatus != DeliveryStatus.DELIVERED) {
                MeshLogger.w(TAG, "Rejected transition for $messageId from terminal state $currentStatus to $targetStatus")
                return false
            }

            val currentPriority = getStatusPriority(currentStatus)
            val targetPriority = getStatusPriority(targetStatus)
            if (targetPriority < currentPriority) {
                MeshLogger.w(TAG, "Rejected downgrade transition for $messageId from $currentStatus (priority $currentPriority) to $targetStatus (priority $targetPriority)")
                return false
            }
        }

        val rowsUpdated = if (allowedPreviousStatuses.isNotEmpty()) {
            val count = chatDao.updateMessageStatusConditional(messageId, targetStatus, allowedPreviousStatuses)
            if (count == 0 && currentStatus != null && allowedPreviousStatuses.contains(currentStatus)) {
                chatDao.updateMessageStatus(messageId, targetStatus)
                1
            } else {
                count
            }
        } else {
            chatDao.updateMessageStatus(messageId, targetStatus)
            1
        }

        if (rowsUpdated > 0) {
            MeshLogger.d(TAG, "Transitioned message $messageId: ${currentStatus ?: "NEW"} -> $targetStatus")
            return true
        } else {
            val freshStatus = chatDao.getMessageByUuid(messageId)?.status
            MeshLogger.w(TAG, "Atomic CAS transition failed for $messageId to $targetStatus. Current DB status: $freshStatus")
            return false
        }
    }

    suspend fun transitionToPending(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.PENDING,
            listOf(DeliveryStatus.PENDING)
        )
    }

    suspend fun transitionToQueued(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.QUEUED,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.FAILED, DeliveryStatus.PERMANENT_FAILURE)
        )
    }

    suspend fun transitionToSending(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.SENDING,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.SENDING)
        )
    }

    suspend fun transitionToSent(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.SENT,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.SENT)
        )
    }

    suspend fun transitionToWaitingForAck(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.WAITING_FOR_ACK,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.WAITING_FOR_ACK)
        )
    }

    suspend fun transitionToRetrying(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.RETRYING,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.RETRYING)
        )
    }

    suspend fun transitionToWaitingForRoute(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.WAITING_FOR_ROUTE,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.WAITING_FOR_ROUTE)
        )
    }

    suspend fun transitionToDelivered(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.DELIVERED,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.SENT, DeliveryStatus.RELAYED, DeliveryStatus.DELIVERED)
        )
    }

    suspend fun transitionToSeen(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.SEEN,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.SENT, DeliveryStatus.RELAYED, DeliveryStatus.DELIVERED, DeliveryStatus.SEEN)
        )
    }

    suspend fun transitionToExpired(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.EXPIRED,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.SENT)
        )
    }

    suspend fun transitionToCancelled(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.CANCELLED,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.SENT)
        )
    }

    suspend fun transitionToPermanentFailure(messageId: String): Boolean {
        return transitionTo(
            messageId,
            DeliveryStatus.PERMANENT_FAILURE,
            listOf(DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.SENDING, DeliveryStatus.WAITING_FOR_ROUTE, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK, DeliveryStatus.SENT)
        )
    }

    suspend fun transitionToFailed(messageId: String): Boolean {
        return transitionToPermanentFailure(messageId)
    }
}
