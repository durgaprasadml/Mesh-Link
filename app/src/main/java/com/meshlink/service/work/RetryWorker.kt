package com.meshlink.service.work

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.data.mapper.toDomain
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chatDao: ChatDao,
    private val meshRepository: MeshRepository,
    private val retryCoordinator: com.meshlink.common.recovery.RetryCoordinator,
    private val stateMachine: com.meshlink.messaging.data.MessageStateMachine
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RetryWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        MeshLogger.d(TAG, "Starting periodic retry worker...")
        try {
            val nonTerminalStatuses = listOf(
                DeliveryStatus.QUEUED,
                DeliveryStatus.RETRYING,
                DeliveryStatus.WAITING_FOR_ROUTE,
                DeliveryStatus.WAITING_FOR_ACK,
                DeliveryStatus.SENDING,
                DeliveryStatus.PENDING
            )
            val pendingMessages = nonTerminalStatuses.flatMap { status ->
                chatDao.getMessagesByStatus(status)
            }.filter { it.isFromMe }.distinctBy { it.messageId }
            
            if (pendingMessages.isEmpty()) {
                MeshLogger.d(TAG, "No pending messages to retry.")
                return@withContext Result.success()
            }
            
            MeshLogger.d(TAG, "Found ${pendingMessages.size} pending messages to check for retry.")
            
            val now = System.currentTimeMillis()
            val (expiredMessages, activeMessages) = pendingMessages.partition { msg ->
                now - msg.timestamp > com.meshlink.common.recovery.RetryCoordinator.MESSAGE_TTL_MS
            }

            for (expired in expiredMessages) {
                MeshLogger.w(TAG, "Message ${expired.messageId} reached 10m TTL. Transitioning to EXPIRED.")
                retryCoordinator.cancelRetryForPacket(expired.messageId)
                stateMachine.transitionToExpired(expired.messageId)
            }

            for (message in activeMessages) {
                MeshLogger.d(TAG, "Retrying active message ${message.messageId} via RetryCoordinator")
                try {
                    retryCoordinator.triggerImmediateRetryForPacket(message.messageId)
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to retry message ${message.messageId}", e)
                }
            }
            
            MeshLogger.d(TAG, "Retry worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Retry worker failed", e)
            Result.retry()
        }
    }
}
