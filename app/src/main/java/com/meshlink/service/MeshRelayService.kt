package com.meshlink.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.SystemClock
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.meshlink.MainActivity
import com.meshlink.data.repository.BleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the mesh relay alive when the app is minimized or closed.
 *
 * FIX Issue 5: Keeps BLE advertising + scanning active so the device is visible
 *              in nearby devices even when the app UI is not open.
 * FIX Issue 6: Incoming messages are processed by BleRepository's init{} collector
 *              which runs on IO dispatcher and fires NotificationHelper for system notifications.
 *              This service ensures that collector stays alive.
 *
 * Key behaviors:
 * - Maintains BLE advertising + scanning + GATT server
 * - Shows a persistent "Mesh Relay Active" notification
 * - Survives screen lock and Doze mode via WakeLock
 * - Auto-restarts if killed by system (START_STICKY + AlarmManager)
 * - Periodically refreshes BLE advertising to prevent OS from killing it
 */
@AndroidEntryPoint
class MeshRelayService : Service() {

    companion object {
        private const val TAG = "MeshRelayService"
        private const val CHANNEL_ID = "mesh_relay_channel"
        private const val NOTIFICATION_ID = 7001
        const val ACTION_START = "com.meshlink.START_RELAY"
        const val ACTION_STOP = "com.meshlink.STOP_RELAY"
        // Refresh BLE every 2 minutes to keep it alive on aggressive OEMs
        private const val BLE_REFRESH_INTERVAL_MS = 120_000L
    }

    @Inject
    lateinit var bleRepository: BleRepository

    private var serviceJob: Job? = null
    private var restartOnDestroy = true
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        Log.d(TAG, "MeshRelayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stopping relay service")
                restartOnDestroy = false
                bleRepository.stopMesh()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification())
                }
                startMeshRelay()
                return START_STICKY
            }
        }
    }

    private fun startMeshRelay() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            try {
                bleRepository.autoStartMesh()
                Log.d(TAG, "Mesh relay started in background")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start mesh relay: ${e.message}")
            }
        }

        // FIX Issue 5: Periodically refresh BLE advertising/scanning to prevent
        // aggressive OEM battery managers from killing BLE.
        // Also renews the WakeLock so it never expires mid-session.
        serviceScope.launch {
            while (isActive) {
                delay(BLE_REFRESH_INTERVAL_MS)
                try {
                    bleRepository.autoStartMesh()
                    renewWakeLock() // Renew every 2 min — lock was acquired for 10 min max
                    Log.d(TAG, "BLE refresh cycle completed")
                } catch (e: Exception) {
                    Log.w(TAG, "BLE refresh failed: ${e.message}")
                }
            }
        }
    }

    // FIX Issue 6: WakeLock prevents CPU from sleeping so BLE callbacks
    // can fire and process incoming messages even in deep sleep
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeshLink::MeshRelayWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes initial acquisition
            }
        }
    }

    /** Renew the WakeLock for another 10-minute window. Called every 2 minutes from BLE refresh. */
    private fun renewWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        // Release old lock and acquire a fresh one to reset the timer
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MeshLink::MeshRelayWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
            wakeLock = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesh Relay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the mesh network active for message relaying"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MeshRelayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("🔗 Mesh Relay Active")
            .setContentText("Relaying messages across the mesh network")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                Notification.Action.Builder(
                    null, "Stop Relay", stopPendingIntent
                ).build()
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (restartOnDestroy) {
            scheduleRestart()
        }
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        bleRepository.stopMesh()
        releaseWakeLock()
        if (restartOnDestroy) {
            scheduleRestart()
        }
        Log.d(TAG, "MeshRelayService destroyed")
        super.onDestroy()
    }

    private fun scheduleRestart() {
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        val restartIntent = Intent(this, MeshRelayService::class.java).apply {
            action = ACTION_START
        }
        val restartPendingIntent = PendingIntent.getService(
            this,
            2,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 3000L,
            restartPendingIntent
        )
    }
}
