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
        internal val SUPPORTED_BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    @Inject
    lateinit var meshLifecycleManager: MeshLifecycleManager

    @android.annotation.SuppressLint("NewApi")
    override fun onReceive(context: Context, intent: Intent) {
        handleBootIntent(context, intent.action)
    }

    @android.annotation.SuppressLint("NewApi")
    internal fun handleBootIntent(context: Context, action: String?) {
        if (action == null || action !in SUPPORTED_BOOT_ACTIONS) {
            MeshLogger.d(TAG, "Ignoring non-boot broadcast action: $action")
            return
        }

        val userManager = try {
            context.getSystemService(android.os.UserManager::class.java)
        } catch (_: Exception) {
            null
        }
        if (userManager != null && !userManager.isUserUnlocked) {
            MeshLogger.w(TAG, "Skipping boot initialization before user unlock (Direct Boot active)")
            return
        }

        MeshLogger.d(TAG, "Boot completed broadcast received action: $action")

        try {
            if (::meshLifecycleManager.isInitialized) {
                meshLifecycleManager.initialize()
                meshLifecycleManager.startMesh()
            }
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
            this.action = MeshBackgroundService.ACTION_START
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
