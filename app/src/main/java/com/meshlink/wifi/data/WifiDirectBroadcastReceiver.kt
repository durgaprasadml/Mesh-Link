package com.meshlink.wifi.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectBroadcastReceiver @Inject constructor(
    private val wifiDirectManager: WifiDirectManager
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiDirectBroadcastReceiver"
    }

    private var isRegistered = false

    fun getIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }

    fun register(context: Context) {
        if (isRegistered) return
        try {
            context.registerReceiver(this, getIntentFilter())
            isRegistered = true
            MeshLogger.d(TAG, "WifiDirectBroadcastReceiver dynamically registered")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to register WifiDirectBroadcastReceiver: ${e.message}")
        }
    }

    fun unregister(context: Context) {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(this)
            isRegistered = false
            MeshLogger.d(TAG, "WifiDirectBroadcastReceiver unregistered")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to unregister WifiDirectBroadcastReceiver: ${e.message}")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        MeshLogger.d(TAG, "Broadcast received action: $action")

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                wifiDirectManager.setP2pEnabled(isEnabled)
                MeshLogger.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION: isEnabled=$isEnabled")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                MeshLogger.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION received. Requesting updated peer list...")
                wifiDirectManager.requestPeers()
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo: NetworkInfo? = @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                MeshLogger.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: networkInfo=$networkInfo")
                wifiDirectManager.onNetworkStateChanged(networkInfo)
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val device: WifiP2pDevice? = @Suppress("DEPRECATION") intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                if (device != null) {
                    MeshLogger.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: name=${device.deviceName}, addr=${device.deviceAddress}")
                }
            }
        }
    }
}
