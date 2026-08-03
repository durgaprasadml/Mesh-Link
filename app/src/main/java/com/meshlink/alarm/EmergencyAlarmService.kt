package com.meshlink.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshlink.R
import com.meshlink.common.logger.MeshLogger

class EmergencyAlarmService : Service() {

    companion object {
        private const val TAG = "EmergencyAlarmService"
        const val CHANNEL_ID = "emergency_alarm_channel"
        const val NOTIFICATION_ID = 9999

        const val ACTION_START_ALARM = "com.meshlink.alarm.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.meshlink.alarm.ACTION_STOP_ALARM"

        fun startService(context: Context) {
            val intent = Intent(context, EmergencyAlarmService::class.java).apply {
                action = ACTION_START_ALARM
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to start foreground service", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EmergencyAlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to stop foreground service", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        MeshLogger.d(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP_ALARM) {
            stopForegroundAndRemove()
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error in startForeground", e)
        }

        return START_STICKY
    }

    private fun stopForegroundAndRemove() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping foreground", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority full-screen emergency alert notifications"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val fullScreenIntent = Intent(this, EmergencyAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acknowledgeIntent = Intent(this, EmergencyAlarmReceiver::class.java).apply {
            action = EmergencyAlarmReceiver.ACTION_ACKNOWLEDGE
        }
        val acknowledgePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            acknowledgeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, EmergencyAlarmReceiver::class.java).apply {
            action = EmergencyAlarmReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 EMERGENCY ALERT")
            .setContentText("Emergency alarm is active.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Acknowledge", acknowledgePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()
    }
}
