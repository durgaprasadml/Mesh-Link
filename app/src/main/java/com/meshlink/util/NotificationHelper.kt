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

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // Tap-to-open: launch MainActivity and navigate to the correct chat
        val openChatIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("address", senderId)
            putExtra("name", senderName)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            openChatIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(senderId.hashCode(), notification)
    }
}
