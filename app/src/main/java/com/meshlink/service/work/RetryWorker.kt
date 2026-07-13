package com.meshlink.service.work

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RetryWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        MeshLogger.d(TAG, "Starting periodic retry worker...")
        try {
            // TODO: In a later phase, inject MeshRepository or RoutingRepository here to retry pending messages.
            // For now, this placeholder guarantees the background task is running safely.
            MeshLogger.d(TAG, "Retry worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Retry worker failed", e)
            Result.retry()
        }
    }
}
