package com.meshlink.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.meshlink.common.logger.MeshLogger
import com.meshlink.MainActivity
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.ui.components.hasRequiredPermissions
import com.meshlink.wifi.data.WifiDirectManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

@AndroidEntryPoint
class MeshRelayService : Service() {

    enum class ServiceState {
        STOPPED, STARTING, RUNNING, RECOVERING, ERROR
    }

    companion object {
        private const val TAG = "MeshRelayService"
        private const val CHANNEL_ID = "mesh_relay_channel"
        private const val NOTIFICATION_ID = 7001
        const val ACTION_START = "com.meshlink.START_RELAY"
        const val ACTION_STOP = "com.meshlink.STOP_RELAY"
        private const val BLE_REFRESH_INTERVAL_MS = 120_000L

        private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()
    }

    @Inject
    lateinit var meshRepository: MeshRepository
    
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
        MeshLogger.d(TAG, "MeshRelayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MeshLogger.d(TAG, "Stopping relay service")
                restartOnDestroy = false
                _serviceState.value = ServiceState.STOPPED
                meshRepository.stopMesh()
                releaseWakeLock()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                _serviceState.value = ServiceState.STARTING
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
                } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
                    MeshLogger.e(TAG, "Not allowed to start foreground service from background", e)
                    _serviceState.value = ServiceState.ERROR
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to start foreground service", e)
                    _serviceState.value = ServiceState.ERROR
                }
                
                if (hasRequiredPermissions(this)) {
                    startMeshRelay()
                    _serviceState.value = ServiceState.RUNNING
                } else {
                    MeshLogger.w(TAG, "Cannot start mesh relay: missing permissions")
                    _serviceState.value = ServiceState.ERROR
                }
                
                return START_STICKY
            }
        }
    }

    private fun startMeshRelay() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            try {
                meshRepository.autoStartMesh()
                MeshLogger.d(TAG, "Mesh relay started in background")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to start mesh relay: ${e.message}")
            }
            
            while (isActive) {
                delay(BLE_REFRESH_INTERVAL_MS)
                try {
                    meshRepository.autoStartMesh()
                    renewWakeLock()
                    MeshLogger.d(TAG, "BLE refresh cycle completed")
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "BLE refresh failed: ${e.message}")
                }
            }
        }
    }

    private fun acquireWakeLock() {
                if (wakeLock == null) {
            try {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "MeshLink::MeshRelayWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(30 * 1000L) // 30 seconds max for a sync pulse
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to acquire WakeLock", e)
            }
        }
    }

    private fun renewWakeLock() {
        releaseWakeLock()
        acquireWakeLock()
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to release WakeLock", e)
        } finally {
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
        _serviceState.value = ServiceState.STOPPED
        serviceScope.cancel()
        serviceJob?.cancel()
        meshRepository.stopMesh()
        wifiDirectManager.unregisterReceiver()
        releaseWakeLock()
        if (restartOnDestroy) {
            scheduleRestart()
        }
        MeshLogger.d(TAG, "MeshRelayService destroyed")
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
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 3000L,
                restartPendingIntent
            )
        } catch (e: SecurityException) {
            MeshLogger.w(TAG, "Exact alarm permission missing. Falling back to inexact alarm.")
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 3000L,
                restartPendingIntent
            )
        }
    }
}
