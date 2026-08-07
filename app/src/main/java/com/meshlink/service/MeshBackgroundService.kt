package com.meshlink.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import com.meshlink.MainActivity
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.common.logger.MeshLogger
import com.meshlink.ui.components.hasRequiredPermissions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MeshBackgroundService : Service() {

    enum class ServiceState {
        STOPPED, STARTING, RUNNING, RECOVERING, ERROR
    }

    companion object {
        private const val TAG = "MeshBackgroundService"
        private const val CHANNEL_ID = "mesh_background_channel"
        private const val NOTIFICATION_ID = 8001
        const val ACTION_START = "com.meshlink.START_MESH_BACKGROUND"
        const val ACTION_STOP = "com.meshlink.STOP_MESH_BACKGROUND"

        private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()
    }

    @Inject
    lateinit var meshLifecycleManager: MeshLifecycleManager

    @Inject
    lateinit var discoveryEngine: DiscoveryEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var restartOnDestroy = true

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1)
                if (state == android.bluetooth.BluetoothAdapter.STATE_ON) {
                    MeshLogger.d(TAG, "Bluetooth turned ON: restoring mesh lifecycle")
                    if (hasRequiredPermissions(this@MeshBackgroundService)) {
                        meshLifecycleManager.startMesh()
                    }
                } else if (state == android.bluetooth.BluetoothAdapter.STATE_OFF) {
                    MeshLogger.d(TAG, "Bluetooth turned OFF: pausing mesh lifecycle")
                    meshLifecycleManager.pauseMesh()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
        MeshLogger.d(TAG, "MeshBackgroundService created")
    }

    @android.annotation.SuppressLint("NewApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MeshLogger.d(TAG, "Stopping MeshBackgroundService")
                restartOnDestroy = false
                _serviceState.value = ServiceState.STOPPED
                meshLifecycleManager.stopMesh()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
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
                            buildNotification(discoveryEngine.scannedDevices.value.size),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification(discoveryEngine.scannedDevices.value.size))
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

                meshLifecycleManager.initialize()
                meshLifecycleManager.startMesh()
                _serviceState.value = ServiceState.RUNNING

                // Observe nearby count to dynamically update notification
                serviceScope.launch {
                    discoveryEngine.scannedDevices.collect { map ->
                        updateNotification(map.size)
                    }
                }

                return START_STICKY
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesh Network Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the decentralized offline mesh network active and discoverable"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(nearbyCount: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(nearbyCount))
    }

    private fun buildNotification(nearbyCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MeshBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (nearbyCount > 0) {
            "🟢 Mesh Active • Nearby Devices: $nearbyCount"
        } else {
            "🟢 Mesh Active • Scanning for nearby devices..."
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh-Link Network Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                Notification.Action.Builder(
                    null, "Stop Service", stopPendingIntent
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
        meshLifecycleManager.stopMesh()
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (_: Exception) {}
        if (restartOnDestroy) {
            scheduleRestart()
        }
        MeshLogger.d(TAG, "MeshBackgroundService destroyed")
        super.onDestroy()
    }

    private fun scheduleRestart() {
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        val restartIntent = Intent(this, MeshBackgroundService::class.java).apply {
            action = ACTION_START
        }
        val restartPendingIntent = PendingIntent.getService(
            this, 2, restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 3000L,
                restartPendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 3000L,
                3000L,
                restartPendingIntent
            )
        }
    }
}
