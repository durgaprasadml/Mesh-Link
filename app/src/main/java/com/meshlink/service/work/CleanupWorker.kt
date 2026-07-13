package com.meshlink.service.work

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshlink.database.data.local.AuditLogDao
import com.meshlink.database.data.local.RelayDao
import com.meshlink.storage.data.local.CacheManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cacheManager: CacheManager,
    private val relayDao: RelayDao,
    private val auditLogDao: AuditLogDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CleanupWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        MeshLogger.d(TAG, "Starting periodic cleanup worker...")
        try {
            // 1. Purge expired relay packets
            relayDao.deleteExpiredPackets(System.currentTimeMillis())
            
            // 2. Prune old audit logs (keep only 10,000)
            val currentLogCount = auditLogDao.getAuditLogCount()
            if (currentLogCount > 10000) {
                auditLogDao.deleteOldestLogs(currentLogCount - 10000)
            }
            
            // 3. Clear file cache quotas
            cacheManager.enforceQuotas()
            cacheManager.clearTemporaryChunks()
            MeshLogger.d(TAG, "Cleanup worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Cleanup worker failed", e)
            Result.retry()
        }
    }
}
