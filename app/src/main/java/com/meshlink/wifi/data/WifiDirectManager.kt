package com.meshlink.wifi.data

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
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.repository.SettingsRepository
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: FirebaseAnalytics,
    private val settingsRepository: SettingsRepository
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
                    MeshLogger.d(TAG, "P2P State Changed: Enabled=$isWifiP2pEnabled")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers ->
                        val peerMap = peers.deviceList.associateBy { it.deviceAddress }
                        _discoveredPeers.value = peerMap
                        MeshLogger.d(TAG, "P2P Peers Changed: Found ${peerMap.size} peers")
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager?.requestConnectionInfo(channel) { info ->
                            _connectionInfo.value = info
                            MeshLogger.d(TAG, "P2P Connected! Group Owner: ${info.isGroupOwner}")
                            
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
                        MeshLogger.d(TAG, "P2P Disconnected")
                        _connectionInfo.value = null
                        _connectedPeerMac.value = null
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    _localDeviceMac.value = device?.deviceAddress
                    MeshLogger.d(TAG, "Local P2P Device MAC: ${_localDeviceMac.value}")
                }
            }
        }
    }

    private var isReceiverRegistered = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        channel = manager?.initialize(context, context.mainLooper, null)
    }

    fun registerReceiver() {
        if (!isReceiverRegistered) {
            context.registerReceiver(receiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignored
            }
            isReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        scope.launch {
            if (!settingsRepository.isWifiDirectEnabled.first() || !settingsRepository.wifiPeerDiscoveryEnabled.first()) {
                MeshLogger.d(TAG, "Wi-Fi Direct or Discovery disabled by settings. Not starting discovery.")
                return@launch
            }
            registerReceiver()
            try {
                manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        MeshLogger.d(TAG, "P2P Discovery Started")
                        // Auto-restart discovery after 110 seconds
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            MeshLogger.d(TAG, "Restarting P2P Discovery (timeout)")
                            startDiscovery()
                        }, 110000L)
                    }
                override fun onFailure(reasonCode: Int) {
                    MeshLogger.e(TAG, "P2P Discovery Failed: $reasonCode")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startDiscovery()
                    }, 10000L)
                }
            })
        } catch (e: SecurityException) {
            MeshLogger.e(TAG, "SecurityException: Missing WiFi Direct permission", e)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Exception starting WiFi discovery: ${e.message}", e)
        }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(deviceAddress: String) {
        scope.launch {
            if (!settingsRepository.isWifiDirectEnabled.first() || !settingsRepository.wifiAutoConnect.first()) {
                MeshLogger.d(TAG, "Wi-Fi Direct connect skipped due to settings")
                return@launch
            }
            val forceGo = settingsRepository.wifiPreferredGroupOwner.first()

            val config = WifiP2pConfig().apply {
                this.deviceAddress = deviceAddress
                
                if (forceGo) {
                    groupOwnerIntent = 15
                } else {
                    // Deterministic Group Owner election based on MAC address comparison
                    val localMac = _localDeviceMac.value
                    if (localMac != null) {
                        // Higher MAC becomes Group Owner (intent = 15), lower becomes client (intent = 0)
                        groupOwnerIntent = if (localMac > deviceAddress) 15 else 0
                    }
                }
            }
        
        try {
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    MeshLogger.d(TAG, "P2P Connect Initiated to $deviceAddress")
                }
                override fun onFailure(reason: Int) {
                    MeshLogger.e(TAG, "P2P Connect Failed to $deviceAddress: $reason")
                }
            })
        } catch (e: SecurityException) {
            MeshLogger.e(TAG, "SecurityException: Missing WiFi Direct permission for connect", e)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Exception connecting to WiFi peer: ${e.message}", e)
        }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    MeshLogger.d(TAG, "P2P Disconnected successfully")
                }
                override fun onFailure(reason: Int) {
                    MeshLogger.e(TAG, "P2P Disconnect failed: $reason")
                }
            })
        } catch (e: SecurityException) {
            MeshLogger.e(TAG, "SecurityException: Missing WiFi Direct permission for disconnect", e)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Exception disconnecting from WiFi peer: ${e.message}", e)
        }
    }
}
