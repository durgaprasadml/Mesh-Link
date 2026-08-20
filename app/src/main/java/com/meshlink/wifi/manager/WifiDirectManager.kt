package com.meshlink.wifi.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.service.RadioState
import com.meshlink.wifi.receiver.WifiP2pBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiP2pManagerFacade: WifiP2pManagerFacade
) {
    companion object {
        private const val TAG = "WifiDirectManager"
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val _radioState = MutableStateFlow(RadioState.STOPPED)
    val radioState: StateFlow<RadioState> = _radioState.asStateFlow()

    private var isSubsystemStarted = false
    private var isReceiverRegistered = false

    val isWifiEnabled: Boolean
        get() = wifiManager?.isWifiEnabled == true

    val isConnected: Boolean
        get() = wifiP2pManagerFacade.isConnected()

    suspend fun ensureConnected(targetDeviceAddress: String? = null, timeoutMs: Long = 5000L): Boolean {
        if (!isWifiEnabled) return false
        if (_radioState.value != RadioState.RUNNING && _radioState.value != RadioState.INITIALIZING) {
            startWifiDirect()
        }
        return wifiP2pManagerFacade.ensureConnected(targetDeviceAddress, timeoutMs)
    }

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    MeshLogger.d(TAG, "WIFI_STATE_CHANGED_ACTION: state=$state (isWifiEnabled=$isWifiEnabled)")
                    if (state == WifiManager.WIFI_STATE_ENABLED) {
                        if (isSubsystemStarted && _radioState.value != RadioState.RUNNING && _radioState.value != RadioState.INITIALIZING) {
                            MeshLogger.d(TAG, "Wi-Fi enabled dynamically at runtime. Starting Wi-Fi Direct...")
                            startWifiDirect()
                        }
                    } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                        MeshLogger.d(TAG, "Wi-Fi disabled dynamically at runtime. Stopping Wi-Fi Direct...")
                        stopWifiDirectInternal()
                    }
                }

                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    MeshLogger.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION: isEnabled=$isEnabled")
                    wifiP2pManagerFacade.onStateChanged(isEnabled)
                    if (isEnabled && isSubsystemStarted && _radioState.value != RadioState.RUNNING && _radioState.value != RadioState.INITIALIZING) {
                        startWifiDirect()
                    } else if (!isEnabled) {
                        stopWifiDirectInternal()
                    }
                }
            }
        }
    }

    init {
        registerReceiverIfNeeded()
    }

    @Synchronized
    private fun registerReceiverIfNeeded() {
        if (!isReceiverRegistered) {
            try {
                val filter = WifiP2pBroadcastReceiver.createIntentFilter().apply {
                    addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                }
                context.registerReceiver(wifiStateReceiver, filter)
                isReceiverRegistered = true
                MeshLogger.d(TAG, "Registered Wi-Fi state broadcast receiver")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to register Wi-Fi state receiver: ${e.message}")
            }
        }
    }

    fun startWifiDirect() {
        isSubsystemStarted = true
        registerReceiverIfNeeded()

        if (!isWifiEnabled) {
            MeshLogger.d(TAG, "Wi-Fi is turned off. Wi-Fi Direct remains STOPPED until Wi-Fi is enabled. BLE mesh remains fully operational.")
            _radioState.value = RadioState.STOPPED
            return
        }

        if (_radioState.value == RadioState.RUNNING || _radioState.value == RadioState.INITIALIZING) return
        _radioState.value = RadioState.INITIALIZING
        MeshLogger.d(TAG, "Starting persistent Wi-Fi Direct Manager")
        try {
            wifiP2pManagerFacade.discoverPeers()
            _radioState.value = RadioState.RUNNING
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start Wi-Fi Direct: ${e.message}")
            _radioState.value = RadioState.FAILED
        }
    }

    fun stopWifiDirect() {
        isSubsystemStarted = false
        stopWifiDirectInternal()
    }

    private fun stopWifiDirectInternal() {
        MeshLogger.d(TAG, "Stopping Wi-Fi Direct Manager")
        try {
            wifiP2pManagerFacade.stopPeerDiscovery()
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error stopping Wi-Fi Direct: ${e.message}")
        } finally {
            _radioState.value = RadioState.STOPPED
        }
    }

    fun restartWifiDirect() {
        MeshLogger.d(TAG, "Restarting Wi-Fi Direct Manager")
        _radioState.value = RadioState.RECOVERING
        stopWifiDirectInternal()
        if (isWifiEnabled) {
            startWifiDirect()
        } else {
            _radioState.value = RadioState.STOPPED
        }
    }
}
