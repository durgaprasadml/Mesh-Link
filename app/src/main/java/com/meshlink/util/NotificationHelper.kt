package com.meshlink.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.meshlink.MainActivity
import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(
    val senderName: String,
    val message: String
)

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "mesh_link_messages"
    const val CHANNEL_ID_MESSAGES = "mesh_link_messages"
    const val CHANNEL_ID_SOS = "mesh_link_sos_alerts"
    const val CHANNEL_NAME_SOS = "Emergency SOS Alerts"

    private const val SOS_DEDUP_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
    private const val MAX_SOS_DEDUP_CACHE = 256

    private val notifiedSosEvents = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > MAX_SOS_DEDUP_CACHE
            }
        }
    )

    private var isAppInForeground = false
    private var currentChatId: String? = null

    private val _inAppNotifications = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 64)
    val inAppNotifications = _inAppNotifications.asSharedFlow()

    fun setAppForeground(foreground: Boolean) {
        isAppInForeground = foreground
    }

    fun setCurrentChatId(chatId: String?) {
        currentChatId = chatId
    }

    @VisibleForTesting
    internal fun resetForTesting() {
        synchronized(notifiedSosEvents) {
            notifiedSosEvents.clear()
        }
        isAppInForeground = false
        currentChatId = null
    }

    fun showSosNotification(
        context: Context,
        sosEventId: String,
        senderId: String,
        senderName: String,
        alertText: String,
        latitude: Double? = null,
        longitude: Double? = null,
        batteryPercent: Int? = null,
        avatarUri: String? = null
    ): Boolean {
        val dedupKey = sosEventId.ifBlank { senderId }
        val now = System.currentTimeMillis()
        val isDuplicate = synchronized(notifiedSosEvents) {
            val lastNotified = notifiedSosEvents[dedupKey]
            if (lastNotified != null && (now - lastNotified) < SOS_DEDUP_WINDOW_MS) {
                true
            } else {
                notifiedSosEvents[dedupKey] = now
                false
            }
        }

        if (isDuplicate) {
            MeshLogger.d(TAG, "Suppressed duplicate SOS alert for event $dedupKey from $senderId")
            return false
        }

        if (isAppInForeground) {
            if (currentChatId == senderId) {
                // User is currently viewing this chat; the chat screen updates with the SOS card in real-time.
                return false
            }
            // User is actively using the app on another screen; show standard in-app notification.
            _inAppNotifications.tryEmit(
                InAppNotification(
                    senderName = "🚨 $senderName",
                    message = alertText
                )
            )
            return true
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        // Respect OS notification permission (Android 13+ / API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                MeshLogger.w(TAG, "Cannot display SOS notification: POST_NOTIFICATIONS permission not granted")
                return false
            }
        }

        // Respect global notification settings
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            MeshLogger.w(TAG, "Cannot display SOS notification: notifications disabled for app")
            return false
        }

        // Configure SOS channel with high importance and a short, one-shot vibration pattern (250ms)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID_SOS)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID_SOS,
                    CHANNEL_NAME_SOS,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for incoming emergency SOS broadcasts from mesh peers"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250) // Short, normal one-shot vibration (250ms)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            } else if (existingChannel.importance == NotificationManager.IMPORTANCE_NONE) {
                MeshLogger.w(TAG, "Cannot display SOS notification: channel $CHANNEL_ID_SOS disabled by user")
                return false
            }
        }

        // Tap-to-open: launch MainActivity and navigate to the relevant chat containing the SOS details
        val openChatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("address", senderId)
            putExtra("name", "🚨 $senderName")
        }

        val notifId = (senderId.hashCode() * 31) xor dedupKey.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openChatIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "🚨 Emergency SOS from $senderName"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SOS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(alertText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVibrate(longArrayOf(0, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val resId = com.meshlink.ui.profile.AvatarAssets.getAvatarResId(avatarUri)
        if (resId != null) {
            try {
                val drawable = ContextCompat.getDrawable(context, resId)
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

        notificationManager.notify(notifId, builder.build())
        return true
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

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            // Tap-to-open: launch MainActivity and navigate to the correct chat
            val openChatIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("address", senderId)
                putExtra("name", senderName)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                senderId.hashCode(),
                openChatIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(senderName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Optional Avatar Bitmap LargeIcon
            val resId = com.meshlink.ui.profile.AvatarAssets.getAvatarResId(avatarUri)
            if (resId != null) {
                try {
                    val drawable = ContextCompat.getDrawable(context, resId)
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
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to display message notification: ${e.message}")
        }
    }
}
