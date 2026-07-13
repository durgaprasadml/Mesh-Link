package com.meshlink.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.meshlink.common.logger.MeshLogger

/**
 * FIX Issue 5 & 6: Auto-start the MeshRelayService after device boot.
 * This ensures BLE advertising + scanning + GATT server are active
 * even when the user hasn't opened the app, making the device
 * visible in nearby devices and able to receive/relay messages.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            MeshLogger.d(TAG, "Boot completed — starting MeshRelayService")
            val serviceIntent = Intent(context, MeshRelayService::class.java).apply {
                action = MeshRelayService.ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to start MeshRelayService on boot: ${e.message}")
            }
        }
    }
}
