package com.meshlink.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var meshLifecycleManager: MeshLifecycleManager

    @android.annotation.SuppressLint("NewApi")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            MeshLogger.d(TAG, "Boot completed broadcast received action: ${intent.action}")

            try {
                meshLifecycleManager.initialize()
                meshLifecycleManager.startMesh()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to initialize MeshLifecycleManager on boot: ${e.message}")
            }

            // Always enqueue WorkManager fallback to guarantee background service restoration
            try {
                val recoveryWork = OneTimeWorkRequestBuilder<MeshRecoveryWorker>().build()
                WorkManager.getInstance(context).enqueue(recoveryWork)
                MeshLogger.d(TAG, "Enqueued MeshRecoveryWorker on boot completion")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to enqueue WorkManager recovery on boot: ${e.message}")
            }

            val serviceIntent = Intent(context, MeshBackgroundService::class.java).apply {
                action = MeshBackgroundService.ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
                MeshLogger.e(TAG, "Foreground service start not allowed from boot receiver: ${e.message}")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to start MeshBackgroundService on boot: ${e.message}")
            }
        }
    }
}
