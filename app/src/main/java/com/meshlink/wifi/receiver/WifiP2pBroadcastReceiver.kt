package com.meshlink.wifi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.wifi.manager.WifiP2pManagerFacade

class WifiP2pBroadcastReceiver(
    private val facade: WifiP2pManagerFacade
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiP2pBroadcastReceiver"

        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                MeshLogger.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION: isEnabled=$isEnabled")
                facade.onStateChanged(isEnabled)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                MeshLogger.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION")
                facade.onPeersChanged()
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = @Suppress("DEPRECATION") intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                MeshLogger.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: isConnected=${networkInfo?.isConnected}")
                facade.onConnectionChanged(networkInfo)
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val device = @Suppress("DEPRECATION") intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                MeshLogger.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: ${device?.deviceName}")
                facade.onThisDeviceChanged(device)
            }
        }
    }
}
