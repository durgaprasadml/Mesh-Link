package com.meshlink.wifi.manager

import android.annotation.SuppressLint
import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.wifi.model.WifiP2pDeviceModel
import com.meshlink.wifi.model.WifiP2pState
import com.meshlink.wifi.permission.WifiP2pPermissionHandler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class WifiP2pManagerFacade @Inject constructor(
    private val context: Context,
    private val wifiP2pManager: WifiP2pManager?,
    private val channel: WifiP2pManager.Channel?,
    private val permissionHandler: WifiP2pPermissionHandler,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "WifiP2pManagerFacade"
        private const val PEER_STALE_THRESHOLD_MS = 60_000L
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        private const val DEFAULT_GO_INTENT = 7 // Mid-range for balanced Group Owner negotiation
    }

    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDeviceModel>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDeviceModel>> = _discoveredPeers.asStateFlow()

    private val _p2pState = MutableStateFlow<WifiP2pState>(WifiP2pState.Disabled)
    val p2pState: StateFlow<WifiP2pState> = _p2pState.asStateFlow()

    private var connectionTimeoutJob: Job? = null
    private var lastConnectedDeviceAddress: String? = null
    private var isAutoReconnectEnabled = true

    init {
        if (wifiP2pManager == null || channel == null) {
            _p2pState.value = WifiP2pState.Unavailable
            MeshLogger.w(TAG, "WifiP2pManager or Channel unavailable on this device")
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        if (wifiP2pManager == null || channel == null) {
            MeshLogger.w(TAG, "Cannot discover peers: WifiP2pManager unavailable")
            return
        }
        if (!permissionHandler.hasPermissions()) {
            MeshLogger.w(TAG, "Cannot discover peers: Missing required Wi-Fi P2P permissions")
            _p2pState.value = WifiP2pState.Error("Missing Wi-Fi P2P permissions")
            return
        }

        MeshLogger.d(TAG, "Discovery Started")
        _p2pState.value = WifiP2pState.Discovering

        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Peer discovery initiated successfully")
            }

            override fun onFailure(reasonCode: Int) {
                val reason = getReasonString(reasonCode)
                MeshLogger.e(TAG, "Discovery Failed: $reason ($reasonCode)")
                _p2pState.value = WifiP2pState.Error("Peer discovery failed: $reason")
            }
        })
    }

    fun stopPeerDiscovery() {
        if (wifiP2pManager == null || channel == null) return

        MeshLogger.d(TAG, "Stopping peer discovery...")
        wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Discovery Finished")
                if (_p2pState.value is WifiP2pState.Discovering) {
                    _p2pState.value = WifiP2pState.Enabled
                }
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.w(TAG, "Stop peer discovery failed: ${getReasonString(reasonCode)}")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, groupOwnerIntent: Int = DEFAULT_GO_INTENT) {
        if (wifiP2pManager == null || channel == null) return
        if (!permissionHandler.hasPermissions()) return

        MeshLogger.d(TAG, "Connecting to peer: $deviceAddress with GO intent $groupOwnerIntent")
        _p2pState.value = WifiP2pState.Connecting(deviceAddress)
        lastConnectedDeviceAddress = deviceAddress

        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            this.groupOwnerIntent = groupOwnerIntent
        }

        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = applicationScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            if (_p2pState.value is WifiP2pState.Connecting) {
                MeshLogger.w(TAG, "Connection timeout to $deviceAddress. Cancelling connection.")
                cancelConnect()
                _p2pState.value = WifiP2pState.Error("Connection timed out")
            }
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Connect initiation succeeded for $deviceAddress")
            }

            override fun onFailure(reasonCode: Int) {
                connectionTimeoutJob?.cancel()
                val reason = getReasonString(reasonCode)
                MeshLogger.e(TAG, "Connect initiation failed: $reason")
                _p2pState.value = WifiP2pState.Error("Connection failed: $reason")
            }
        })
    }

    fun cancelConnect() {
        if (wifiP2pManager == null || channel == null) return
        wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Connect attempt cancelled successfully")
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.w(TAG, "Cancel connect failed: ${getReasonString(reasonCode)}")
            }
        })
    }

    fun disconnect() {
        if (wifiP2pManager == null || channel == null) return

        MeshLogger.d(TAG, "Disconnect requested")
        connectionTimeoutJob?.cancel()
        removeGroup()
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        if (wifiP2pManager == null || channel == null) return
        if (!permissionHandler.hasPermissions()) return

        MeshLogger.d(TAG, "Creating Wi-Fi P2P Group as Group Owner...")
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Wi-Fi P2P Group creation initiated successfully")
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.e(TAG, "Group creation failed: ${getReasonString(reasonCode)}")
            }
        })
    }

    fun removeGroup() {
        if (wifiP2pManager == null || channel == null) return

        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Group removed successfully")
                _p2pState.value = WifiP2pState.Disconnected
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.w(TAG, "Remove group failed: ${getReasonString(reasonCode)}")
                _p2pState.value = WifiP2pState.Disconnected
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        if (wifiP2pManager == null || channel == null) return
        if (!permissionHandler.hasPermissions()) return

        wifiP2pManager.requestPeers(channel) { peerList: WifiP2pDeviceList? ->
            handlePeerList(peerList)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestConnectionInfo() {
        if (wifiP2pManager == null || channel == null) return

        wifiP2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo? ->
            handleConnectionInfo(info)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGroupInfo() {
        if (wifiP2pManager == null || channel == null) return
        if (!permissionHandler.hasPermissions()) return

        wifiP2pManager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            if (group != null) {
                MeshLogger.d(TAG, "Group Info received: networkName=${group.networkName}, passPhrase=${group.passphrase}, clientListCount=${group.clientList.size}")
            }
        }
    }

    // Callbacks from WifiP2pBroadcastReceiver
    fun onStateChanged(isEnabled: Boolean) {
        if (isEnabled) {
            if (_p2pState.value is WifiP2pState.Disabled || _p2pState.value is WifiP2pState.Unavailable) {
                _p2pState.value = WifiP2pState.Enabled
            }
        } else {
            _p2pState.value = WifiP2pState.Disabled
            _discoveredPeers.value = emptyList()
        }
    }

    fun onPeersChanged() {
        requestPeers()
    }

    fun onConnectionChanged(networkInfo: NetworkInfo?) {
        if (networkInfo?.isConnected == true) {
            requestConnectionInfo()
            requestGroupInfo()
        } else {
            connectionTimeoutJob?.cancel()
            val previousState = _p2pState.value
            _p2pState.value = WifiP2pState.Disconnected

            if (previousState is WifiP2pState.Connected && isAutoReconnectEnabled && lastConnectedDeviceAddress != null) {
                MeshLogger.d(TAG, "Link dropped. Triggering automatic reconnect attempt to $lastConnectedDeviceAddress")
                triggerAutoReconnect(lastConnectedDeviceAddress!!)
            }
        }
    }

    fun onThisDeviceChanged(device: WifiP2pDevice?) {
        if (device != null) {
            MeshLogger.d(TAG, "Local device details: name=${device.deviceName}, address=${device.deviceAddress}, status=${device.status}")
        }
    }

    private fun handlePeerList(peerList: WifiP2pDeviceList?) {
        if (peerList == null) return

        val now = System.currentTimeMillis()
        val updatedList = peerList.deviceList.map { device ->
            WifiP2pDeviceModel(
                deviceName = device.deviceName.takeIf { it.isNotEmpty() } ?: device.deviceAddress,
                deviceAddress = device.deviceAddress,
                status = device.status,
                isGroupOwner = device.isGroupOwner,
                lastSeen = now
            )
        }

        // Filter duplicate addresses and stale peers
        val existingPeersMap = _discoveredPeers.value.associateBy { it.deviceAddress }.toMutableMap()
        for (peer in updatedList) {
            existingPeersMap[peer.deviceAddress] = peer
            MeshLogger.d(TAG, "Peer Found: ${peer.deviceName} (${peer.deviceAddress}) status=${peer.statusString}")
        }

        // Purge peers not seen for > PEER_STALE_THRESHOLD_MS unless currently connected
        val cleanedList = existingPeersMap.values.filter { peer ->
            (now - peer.lastSeen) <= PEER_STALE_THRESHOLD_MS || peer.status == WifiP2pDevice.CONNECTED
        }

        _discoveredPeers.value = cleanedList
    }

    private fun handleConnectionInfo(info: WifiP2pInfo?) {
        connectionTimeoutJob?.cancel()

        if (info != null && info.groupFormed) {
            val goAddress = info.groupOwnerAddress?.hostAddress ?: ""
            val isGo = info.isGroupOwner
            MeshLogger.d(TAG, "Connected: GroupOwnerAddress=$goAddress, isGroupOwner=$isGo")
            _p2pState.value = WifiP2pState.Connected(groupOwnerAddress = goAddress, isGroupOwner = isGo)
        } else {
            MeshLogger.d(TAG, "Group not formed or connection info null")
        }
    }

    private fun triggerAutoReconnect(targetAddress: String) {
        applicationScope.launch {
            _p2pState.value = WifiP2pState.Recovering
            delay(2000L)
            MeshLogger.d(TAG, "Attempting auto-reconnect to $targetAddress...")
            connect(targetAddress)
        }
    }

    private fun getReasonString(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.ERROR -> "Internal Error"
            WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi P2P Unsupported"
            WifiP2pManager.BUSY -> "Framework Busy"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "No Service Requests"
            else -> "Unknown ($reasonCode)"
        }
    }
}
