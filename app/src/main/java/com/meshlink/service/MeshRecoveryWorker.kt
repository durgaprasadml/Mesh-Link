package com.meshlink.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshlink.common.logger.MeshLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class MeshRecoveryWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val meshLifecycleManager: MeshLifecycleManager
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "MeshRecoveryWorker"
        const val WORK_NAME = "mesh_recovery_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        MeshLogger.d(TAG, "Executing MeshRecoveryWorker background self-healing check")
        try {
            if (!meshLifecycleManager.isMeshRunning()) {
                MeshLogger.d(TAG, "Mesh engine was stopped or killed; restarting via MeshLifecycleManager")
                meshLifecycleManager.initialize()
                meshLifecycleManager.startMesh()
            }

            val serviceIntent = Intent(appContext, MeshBackgroundService::class.java).apply {
                action = MeshBackgroundService.ACTION_START
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(serviceIntent)
                } else {
                    appContext.startService(serviceIntent)
                }
            } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
                MeshLogger.e(TAG, "Foreground service start disallowed by OS background restrictions: ${e.message}")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to start MeshBackgroundService from WorkManager: ${e.message}")
            }

            Result.success()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "MeshRecoveryWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
