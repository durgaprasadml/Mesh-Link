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
    private var currentChatId: String? = null

    private val _inAppNotifications = MutableSharedFlow<InAppNotification>()
    val inAppNotifications = _inAppNotifications.asSharedFlow()

    fun setAppForeground(foreground: Boolean) {
        isAppInForeground = foreground
    }

    fun setCurrentChatId(chatId: String?) {
        currentChatId = chatId
    }

    fun showMessageNotification(
        context: Context,
        senderId: String,
        senderName: String,
        message: String,
        avatarUri: String? = null
    ) {
        if (isAppInForeground) {
            if (currentChatId == senderId) {
                // User Currently Viewing The Same Chat: Do NOT show notification.
                return
            }
            if (currentChatId != null) {
                // User Currently Viewing Another Chat: Show in-app notification.
                _inAppNotifications.tryEmit(InAppNotification(senderName, message))
                return
            }
            // User Actively Using Mesh Link (not in a chat): No notification, just update UI
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.meshlink.R.drawable.ic_notification_info)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Optional Avatar Bitmap LargeIcon
        val resId = com.meshlink.ui.profile.AvatarAssets.getAvatarResId(avatarUri)
        if (resId != null) {
            try {
                val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    builder.setLargeIcon(bitmap)
                }
            } catch (_: Exception) {}
        }

        notificationManager.notify(senderId.hashCode(), builder.build())
    }
}
