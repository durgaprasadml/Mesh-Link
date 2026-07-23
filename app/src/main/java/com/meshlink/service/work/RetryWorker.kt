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
    private val meshRepository: MeshRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RetryWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        MeshLogger.d(TAG, "Starting periodic retry worker...")
        try {
            val pendingMessages = chatDao.getMessagesByStatus(DeliveryStatus.PENDING)
            
            if (pendingMessages.isEmpty()) {
                MeshLogger.d(TAG, "No pending messages to retry.")
                return@withContext Result.success()
            }
            
            MeshLogger.d(TAG, "Found ${pendingMessages.size} pending messages to retry.")
            
            for (message in pendingMessages) {
                val chat = chatDao.getChatById(message.chatId)
                if (chat != null) {
                    MeshLogger.d(TAG, "Retrying message ${message.messageId} to ${chat.name}")
                    try {
                        meshRepository.sendMessage(message.chatId, message.toDomain())
                    } catch (e: Exception) {
                        MeshLogger.e(TAG, "Failed to retry message ${message.messageId}", e)
                    }
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
