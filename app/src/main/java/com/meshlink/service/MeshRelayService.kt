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
import com.meshlink.common.power.PowerState
import com.meshlink.common.power.PowerStateManager
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

    @Inject
    lateinit var powerStateManager: PowerStateManager

    private var serviceJob: Job? = null
    private var restartOnDestroy = true
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1)
                if (state == android.bluetooth.BluetoothAdapter.STATE_ON) {
                    MeshLogger.d(TAG, "Bluetooth turned ON, restarting Mesh Relay")
                    if (hasRequiredPermissions(this@MeshRelayService)) {
                        serviceScope.launch {
                            meshRepository.autoStartMesh()
                        }
                    }
                } else if (state == android.bluetooth.BluetoothAdapter.STATE_OFF) {
                    MeshLogger.d(TAG, "Bluetooth turned OFF, stopping Mesh operations temporarily")
                    meshRepository.stopMesh()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        wifiDirectManager.registerReceiver()
        
        val filter = android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
        
        MeshLogger.d(TAG, "MeshRelayService created")
    }

    @android.annotation.SuppressLint("NewApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MeshLogger.d(TAG, "Stopping relay service")
                restartOnDestroy = false
                _serviceState.value = ServiceState.STOPPED
                meshRepository.stopMesh()
                
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
                    _serviceState.value = ServiceState.STOPPED
                    restartOnDestroy = false
                    stopSelf()
                    return START_NOT_STICKY
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to start foreground service", e)
                    _serviceState.value = ServiceState.STOPPED
                    restartOnDestroy = false
                    stopSelf()
                    return START_NOT_STICKY
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
            
            var lastRefresh = System.currentTimeMillis()
            while (isActive) {
                delay(15_000L)
                if (System.currentTimeMillis() - lastRefresh >= BLE_REFRESH_INTERVAL_MS) {
                    val currentState = powerStateManager.powerState.value
                    if (currentState == PowerState.DOZE_MODE || currentState == PowerState.RESTRICTED) {
                        MeshLogger.d(TAG, "Skipping BLE refresh due to power state: $currentState")
                        lastRefresh = System.currentTimeMillis()
                        continue
                    }
                    withWakeLock(30_000L) {
                        try {
                            meshRepository.autoStartMesh()
                            lastRefresh = System.currentTimeMillis()
                            MeshLogger.d(TAG, "BLE refresh cycle completed under scoped WakeLock")
                        } catch (e: Exception) {
                            MeshLogger.w(TAG, "BLE refresh failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private suspend fun withWakeLock(timeoutMs: Long, block: suspend () -> Unit) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MeshLink::ScopedWakeLock").apply {
            setReferenceCounted(false)
        }
        try {
            lock.acquire(timeoutMs)
            block()
        } finally {
            if (lock.isHeld) {
                lock.release()
            }
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
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            // Ignored
        }
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
            MeshLogger.w(TAG, "Exact alarm permission missing. Falling back to setWindow inexact alarm.")
            alarmManager.setWindow(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 3000L,
                3000L,
                restartPendingIntent
            )
        }
    }
}
