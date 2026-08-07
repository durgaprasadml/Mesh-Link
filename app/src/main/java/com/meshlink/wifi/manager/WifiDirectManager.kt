package com.meshlink.wifi.manager

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.service.RadioState
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

    private val _radioState = MutableStateFlow(RadioState.STOPPED)
    val radioState: StateFlow<RadioState> = _radioState.asStateFlow()

    fun startWifiDirect() {
        if (_radioState.value == RadioState.RUNNING) return
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
        stopWifiDirect()
        startWifiDirect()
    }
}
