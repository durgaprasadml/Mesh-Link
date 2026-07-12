package com.meshlink.data.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: FirebaseAnalytics
) {
    companion object {
        private const val TAG = "WifiDirectManager"
    }

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    // Map of peer MAC address to WifiP2pDevice
    private val _discoveredPeers = MutableStateFlow<Map<String, WifiP2pDevice>>(emptyMap())
    val discoveredPeers: StateFlow<Map<String, WifiP2pDevice>> = _discoveredPeers.asStateFlow()

    // Flow of connection info when a group is formed
    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()
    
    // Connected peer MAC address (if any)
    private val _connectedPeerMac = MutableStateFlow<String?>(null)
    val connectedPeerMac: StateFlow<String?> = _connectedPeerMac.asStateFlow()
    
    private val _localDeviceMac = MutableStateFlow<String?>(null)
    val localDeviceMac: StateFlow<String?> = _localDeviceMac.asStateFlow()

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "P2P State Changed: Enabled=$isWifiP2pEnabled")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers ->
                        val peerMap = peers.deviceList.associateBy { it.deviceAddress }
                        _discoveredPeers.value = peerMap
                        Log.d(TAG, "P2P Peers Changed: Found ${peerMap.size} peers")
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager?.requestConnectionInfo(channel) { info ->
                            _connectionInfo.value = info
                            Log.d(TAG, "P2P Connected! Group Owner: ${info.isGroupOwner}")
                            
                            // Get the connected peer MAC from the group info
                            manager?.requestGroupInfo(channel) { group ->
                                if (group != null) {
                                    if (group.isGroupOwner) {
                                        // We are owner, peer is a client
                                        _connectedPeerMac.value = group.clientList.firstOrNull()?.deviceAddress
                                    } else {
                                        // We are client, peer is owner
                                        _connectedPeerMac.value = group.owner.deviceAddress
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "P2P Disconnected")
                        _connectionInfo.value = null
                        _connectedPeerMac.value = null
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    _localDeviceMac.value = device?.deviceAddress
                    Log.d(TAG, "Local P2P Device MAC: ${_localDeviceMac.value}")
                }
            }
        }
    }

    init {
        channel = manager?.initialize(context, context.mainLooper, null)
    }

    fun registerReceiver() {
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignored
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "P2P Discovery Started")
            }
            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "P2P Discovery Failed: $reasonCode")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(deviceAddress: String) {
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }
        
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "P2P Connect Initiated to $deviceAddress")
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "P2P Connect Failed to $deviceAddress: $reason")
            }
        })
    }
}
