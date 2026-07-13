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
import com.meshlink.data.wifi.WifiDirectManager
import com.meshlink.ui.components.hasRequiredPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeshRelayService : Service() {

    companion object {
        private const val TAG = "MeshRelayService"
        private const val CHANNEL_ID = "mesh_relay_channel"
        private const val NOTIFICATION_ID = 7001
        const val ACTION_START = "com.meshlink.START_RELAY"
        const val ACTION_STOP = "com.meshlink.STOP_RELAY"
        private const val BLE_REFRESH_INTERVAL_MS = 120_000L
    }

    @Inject
    lateinit var bleRepository: BleRepository
    
    @Inject
    lateinit var wifiDirectManager: WifiDirectManager

    private var serviceJob: Job? = null
    private var restartOnDestroy = true
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        wifiDirectManager.registerReceiver()
        Log.d(TAG, "MeshRelayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stopping relay service")
                restartOnDestroy = false
                bleRepository.stopMesh()
                releaseWakeLock()
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            buildNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service", e)
                }
                
                if (hasRequiredPermissions(this)) {
                    startMeshRelay()
                } else {
                    Log.w(TAG, "Cannot start mesh relay: missing permissions")
                }
                
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
            
            while (isActive) {
                delay(BLE_REFRESH_INTERVAL_MS)
                try {
                    bleRepository.autoStartMesh()
                    renewWakeLock()
                    Log.d(TAG, "BLE refresh cycle completed")
                } catch (e: Exception) {
                    Log.w(TAG, "BLE refresh failed: ${e.message}")
                }
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeshLink::MeshRelayWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L)
            }
        }
    }

    private fun renewWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
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
        wifiDirectManager.unregisterReceiver()
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
