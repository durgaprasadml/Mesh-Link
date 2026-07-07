package com.meshlink.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.meshlink.MainActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(
    val senderName: String,
    val message: String
)

object NotificationHelper {
    private const val CHANNEL_ID = "mesh_link_messages"
    private var isAppInForeground = false

    private val _inAppNotifications = MutableSharedFlow<InAppNotification>()
    val inAppNotifications = _inAppNotifications.asSharedFlow()

    fun setAppForeground(foreground: Boolean) {
        isAppInForeground = foreground
    }

    fun showMessageNotification(context: Context, senderId: String, senderName: String, message: String) {
        if (isAppInForeground) {
            _inAppNotifications.tryEmit(InAppNotification(senderName, message))
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationChannel is required for Android O+ (API 26+)
        // Since minSdk is 26, we don't strictly need the check, but it's good practice.
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(senderId.hashCode(), notification)
    }
}
