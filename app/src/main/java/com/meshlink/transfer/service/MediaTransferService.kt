package com.meshlink.transfer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshlink.R
import com.meshlink.common.logger.MeshLogger
import com.meshlink.transfer.MediaTransferSessionManager
import com.meshlink.transfer.TransferState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MediaTransferService : Service() {

    companion object {
        private const val TAG = "MediaTransferService"
        private const val CHANNEL_ID = "mesh_media_transfer_channel"
        private const val NOTIFICATION_ID = 4001

        fun startServiceIfNeeded(context: Context) {
            val intent = Intent(context, MediaTransferService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MediaTransferService::class.java)
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var sessionManager: MediaTransferSessionManager

    // SupervisorJob ensures that a failure in any single child transfer coroutine
    // does not cascade and cancel other active transfers in this scope.
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var collectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting Mesh Media Transfers...", 0, 0f))

        collectorJob = serviceScope.launch {
            sessionManager.activeMetrics.collectLatest { metricsMap ->
                val activeList = metricsMap.values.filter {
                    it.status == TransferState.SENDING ||
                    it.status == TransferState.RECEIVING ||
                    it.status == TransferState.PREPARING ||
                    it.status == TransferState.VERIFYING
                }

                if (activeList.isEmpty()) {
                    MeshLogger.d(TAG, "Transfer queue empty. Automatically stopping Foreground Service.")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val count = activeList.size
                    val totalSpeed = activeList.sumOf { it.currentSpeedBytesPerSec.toDouble() }.toFloat()
                    val avgProgress = activeList.map { it.progress }.average().toFloat()
                    val speedKb = totalSpeed / 1024f

                    val statusText = "Transferring $count file(s) • ${String.format("%.1f", speedKb)} KB/s"
                    val progressPercent = (avgProgress * 100).toInt()

                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText, progressPercent, speedKb))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        collectorJob?.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress for active mesh network file and media transfers"
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, progressPercent: Int, speedKb: Float): android.app.Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Link Media Transfer Engine")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification_transfer)
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
