package com.meshlink.wifi.manager

import android.annotation.SuppressLint
import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.qualifiers.ApplicationContext

data class PeerWifiDetails(
    val meshId: String,
    val macAddress: String = "",
    val deviceName: String = "",
    val ipAddress: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Singleton
class WifiP2pManagerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiP2pManager: WifiP2pManager?,
    private val channel: WifiP2pManager.Channel?,
    private val permissionHandler: WifiP2pPermissionHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val discoveryEngineProvider: javax.inject.Provider<com.meshlink.ble.discovery.DiscoveryEngine>? = null
) {
    companion object {
        private const val TAG = "WifiP2pManagerFacade"
        private const val PEER_STALE_THRESHOLD_MS = 10_000L
        private const val CONNECTION_TIMEOUT_MS = 45_000L
        private const val DEFAULT_GO_INTENT = 7 // Mid-range for balanced Group Owner negotiation
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }

    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDeviceModel>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDeviceModel>> = _discoveredPeers.asStateFlow()

    private val _p2pState = MutableStateFlow<WifiP2pState>(WifiP2pState.Disabled)
    val p2pState: StateFlow<WifiP2pState> = _p2pState.asStateFlow()

    private val _localDeviceName = MutableStateFlow(
        try {
            android.provider.Settings.Global.getString(context.contentResolver, "device_name")
                ?: android.os.Build.MODEL
        } catch (_: Exception) {
            android.os.Build.MODEL
        }
    )
    val localDeviceName: StateFlow<String> = _localDeviceName.asStateFlow()

    private val _localDeviceAddress = MutableStateFlow("")
    val localDeviceAddress: StateFlow<String> = _localDeviceAddress.asStateFlow()

    private val peerWifiMap = ConcurrentHashMap<String, PeerWifiDetails>()
    private val connectionMutex = Mutex()
    private val failedConnectAttempts = ConcurrentHashMap<String, Long>()
    private var lastDiscoverPeersCallTime = 0L

    fun registerPeerWifiDetails(meshId: String, mac: String, name: String, ip: String = "") {
        val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        if (canonicalId.isBlank()) return
        val existing = peerWifiMap[canonicalId]
        val cleanMac = if (mac.isNotBlank() && !mac.contains("02:00:00:00:00:00")) mac.trim() else (existing?.macAddress ?: "")
        val cleanName = if (name.isNotBlank()) name.trim() else (existing?.deviceName ?: "")
        val cleanIp = if (ip.isNotBlank() && ip != "0.0.0.0") ip.trim() else (existing?.ipAddress ?: "")
        val updated = PeerWifiDetails(
            meshId = canonicalId,
            macAddress = cleanMac,
            deviceName = cleanName,
            ipAddress = cleanIp,
            lastUpdated = System.currentTimeMillis()
        )
        peerWifiMap[canonicalId] = updated
        MeshLogger.i(TAG, "Registered peer Wi-Fi Direct details for $canonicalId: name=${updated.deviceName}, mac=${updated.macAddress}, ip=${updated.ipAddress}")
    }

    fun getPeerWifiDetails(meshId: String): PeerWifiDetails? {
        val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        return peerWifiMap[canonicalId]
    }

    private var connectionTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttemptCount = 0
    private var lastConnectedDeviceAddress: String? = null
    private var isAutoReconnectEnabled = true

    init {
        if (wifiP2pManager == null || channel == null) {
            _p2pState.value = WifiP2pState.Unavailable
            MeshLogger.w(TAG, "WifiP2pManager or Channel unavailable on this device")
        } else {
            requestDeviceInfo()
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

        if (_p2pState.value is WifiP2pState.Connecting || _p2pState.value is WifiP2pState.Connected) {
            MeshLogger.d(TAG, "discoverPeers: Skipping discovery while in Connecting or Connected state (${_p2pState.value})")
            return
        }

        val now = System.currentTimeMillis()
        if (_p2pState.value is WifiP2pState.Discovering && (now - lastDiscoverPeersCallTime) < 4_000L) {
            MeshLogger.d(TAG, "discoverPeers: Discovery already active and recent. Debouncing scan call.")
            return
        }
        lastDiscoverPeersCallTime = now

        requestDeviceInfo()
        MeshLogger.i("AUDIO_WIFI_DISCOVERY", "[AUDIO_WIFI_DISCOVERY] Initiating Wi-Fi Direct peer discovery...")
        _p2pState.value = WifiP2pState.Discovering

        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Peer discovery initiated successfully")
            }

            override fun onFailure(reasonCode: Int) {
                val reason = getReasonString(reasonCode)
                MeshLogger.e(TAG, "Discovery Failed: $reason ($reasonCode)")
                if (reasonCode != WifiP2pManager.BUSY) {
                    _p2pState.value = WifiP2pState.Error("Peer discovery failed: $reason")
                }
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

    private val _connectedClientsCount = MutableStateFlow<Int>(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    fun isGroupFormed(): Boolean {
        return _p2pState.value is WifiP2pState.Connected
    }

    fun isConnected(): Boolean {
        return _p2pState.value is WifiP2pState.Connected
    }

    @SuppressLint("MissingPermission")
    fun requestDeviceInfo() {
        if (wifiP2pManager == null || channel == null) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                wifiP2pManager.requestDeviceInfo(channel) { device ->
                    if (device != null) {
                        onThisDeviceChanged(device)
                    }
                }
            } catch (e: Exception) {
                MeshLogger.w(TAG, "requestDeviceInfo failed: ${e.message}")
            }
        }
    }

    private fun resolveTargetPeer(
        peers: List<WifiP2pDeviceModel>,
        canonicalTarget: String?,
        peerDetails: PeerWifiDetails?,
        bleDevice: com.meshlink.domain.model.BleDevice?
    ): String? {
        if (peers.isEmpty()) return null

        val now = System.currentTimeMillis()
        val activePeers = peers.filter { peer ->
            val lastFail = failedConnectAttempts[peer.deviceAddress] ?: 0L
            (now - lastFail) > 4000L
        }
        if (activePeers.isEmpty()) return null

        // 1. Exact MAC match if target or peerDetails contains MAC
        val candidateMacs = mutableListOf<String>()
        if (canonicalTarget?.contains(":") == true) candidateMacs.add(canonicalTarget)
        if (peerDetails?.macAddress?.contains(":") == true && !peerDetails.macAddress.contains("02:00:00:00:00:00")) {
            candidateMacs.add(peerDetails.macAddress)
        }
        for (mac in candidateMacs) {
            val matched = activePeers.firstOrNull { it.deviceAddress.equals(mac, ignoreCase = true) }
            if (matched != null) {
                MeshLogger.i(TAG, "resolveTargetPeer: Matched MAC $mac for target $canonicalTarget (${matched.deviceName})")
                return matched.deviceAddress
            }
        }

        // 2. Candidate names to search for (ordered by specificity)
        val candidateNames = mutableListOf<String>()
        val pdName = peerDetails?.deviceName
        if (!pdName.isNullOrBlank()) candidateNames.add(pdName)
        val bDisp = bleDevice?.displayName
        if (!bDisp.isNullOrBlank()) candidateNames.add(bDisp)
        val bName = bleDevice?.name
        if (!bName.isNullOrBlank()) candidateNames.add(bName)
        val bBtName = bleDevice?.bluetoothDeviceName
        if (!bBtName.isNullOrBlank()) candidateNames.add(bBtName)
        if (!canonicalTarget.isNullOrBlank()) candidateNames.add(canonicalTarget)

        // Helper to normalize strings for comparison: lowercase, remove all non-alphanumeric characters
        fun normalize(str: String): String = str.lowercase().replace(Regex("[^a-z0-9]"), "")

        // Helper to extract words/tokens from a name
        fun extractTokens(str: String): List<String> =
            str.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }

        // A. Exact or Substring name match
        for (cand in candidateNames) {
            val matched = activePeers.firstOrNull {
                it.deviceName.equals(cand, ignoreCase = true) ||
                it.deviceName.contains(cand, ignoreCase = true) ||
                cand.contains(it.deviceName, ignoreCase = true)
            }
            if (matched != null) {
                MeshLogger.i(TAG, "resolveTargetPeer: Substring matched candidate '$cand' with peer '${matched.deviceName}' (${matched.deviceAddress})")
                return matched.deviceAddress
            }
        }

        // B. Normalized name match (e.g. "motoedge60fusion" in "motoedge60fusionfyee", or "motorolaedge60fusion" vs "motoedge60fusion")
        for (cand in candidateNames) {
            val normCand = normalize(cand)
            if (normCand.length >= 3) {
                val matched = activePeers.firstOrNull {
                    val normPeer = normalize(it.deviceName)
                    normPeer.contains(normCand) || normCand.contains(normPeer)
                }
                if (matched != null) {
                    MeshLogger.i(TAG, "resolveTargetPeer: Normalized matched candidate '$cand' ($normCand) with peer '${matched.deviceName}' (${matched.deviceAddress})")
                    return matched.deviceAddress
                }
            }
        }

        // C. Token overlap match (e.g. ["moto", "edge", "60", "fusion"] vs ["motorola", "edge", "60", "fusion"])
        for (cand in candidateNames) {
            val candTokens = extractTokens(cand)
            if (candTokens.isNotEmpty()) {
                val matched = activePeers.firstOrNull { peer ->
                    val peerTokens = extractTokens(peer.deviceName)
                    var matches = 0
                    for (ct in candTokens) {
                        if (peerTokens.any { pt -> pt == ct || (pt.length >= 4 && ct.length >= 4 && (pt.startsWith(ct) || ct.startsWith(pt))) }) {
                            matches++
                        }
                    }
                    matches >= 2 || (candTokens.size == 1 && matches == 1)
                }
                if (matched != null) {
                    MeshLogger.i(TAG, "resolveTargetPeer: Token matched candidate '$cand' with peer '${matched.deviceName}' (${matched.deviceAddress})")
                    return matched.deviceAddress
                }
            }
        }

        // D. Single Peer Fallback: If only 1 peer is discovered in radio range with AVAILABLE, CONNECTED, or INVITED status,
        // or only 1 peer total in discovered peers list, this MUST be our nearby device!
        val availablePeers = activePeers.filter {
            it.status == android.net.wifi.p2p.WifiP2pDevice.AVAILABLE ||
            it.status == android.net.wifi.p2p.WifiP2pDevice.CONNECTED ||
            it.status == android.net.wifi.p2p.WifiP2pDevice.INVITED
        }
        if (availablePeers.size == 1) {
            val single = availablePeers.first()
            MeshLogger.i(TAG, "resolveTargetPeer: Single available Wi-Fi Direct peer found '${single.deviceName}' (${single.deviceAddress}), selecting as fallback for target $canonicalTarget")
            return single.deviceAddress
        } else if (activePeers.size == 1) {
            val single = activePeers.first()
            MeshLogger.i(TAG, "resolveTargetPeer: Single Wi-Fi Direct peer in range '${single.deviceName}' (${single.deviceAddress}), selecting as fallback for target $canonicalTarget")
            return single.deviceAddress
        }

        // E. Multiple peers: if no target specified, pick first available
        if (canonicalTarget == null && availablePeers.isNotEmpty()) {
            return availablePeers.first().deviceAddress
        }

        return null
    }

    suspend fun ensureConnected(
        targetDeviceAddress: String? = null,
        timeoutMs: Long = 45000L,
        isInitiator: Boolean = true
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (isConnected()) return@withContext true

        connectionMutex.withLock {
            if (isConnected()) return@withLock true

            val current = _p2pState.value
            // If we are stuck in an empty autonomous group as Group Owner with 0 clients,
            // remove the group so the radio can cleanly return to discovery mode.
            if (current is WifiP2pState.Connected && current.isGroupOwner && _connectedClientsCount.value == 0) {
                MeshLogger.d(TAG, "ensureConnected: Cleaning up empty autonomous group before discovery...")
                removeGroup()
                kotlinx.coroutines.delay(400)
            }

            if (_p2pState.value is WifiP2pState.Connecting) {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    if (isConnected()) return@withLock true
                    if (_p2pState.value is WifiP2pState.Error || _p2pState.value is WifiP2pState.Disconnected) break
                    kotlinx.coroutines.delay(100)
                }
                if (isConnected()) return@withLock true
            }

            // Trigger discovery
            discoverPeers()

            val canonicalTarget = targetDeviceAddress?.let { com.meshlink.util.MeshIdNormalizer.canonicalize(it) }
            val peerDetails = canonicalTarget?.let { peerWifiMap[it] }
            val bleDevice = canonicalTarget?.let { discoveryEngineProvider?.get()?.scannedDevices?.value?.get(it) }

            val discoveryStartTime = System.currentTimeMillis()
            var lastRequestPeersTime = 0L

            while (System.currentTimeMillis() - discoveryStartTime < timeoutMs) {
                if (isConnected()) return@withLock true

                // Only request scan updates if not currently in active connection negotiation
                if (_p2pState.value !is WifiP2pState.Connecting) {
                    val now = System.currentTimeMillis()
                    if (now - lastRequestPeersTime >= 2000L) {
                        lastRequestPeersTime = now
                        requestPeers()
                    }
                }

                val peers = _discoveredPeers.value
                val target = resolveTargetPeer(peers, canonicalTarget, peerDetails, bleDevice)

                if (target != null && _p2pState.value !is WifiP2pState.Connecting) {
                    MeshLogger.i("AUDIO_WIFI_CONNECTING", "[AUDIO_WIFI_CONNECTING] ensureConnected: Found matching Wi-Fi Direct peer $target for target=$canonicalTarget (isInitiator=$isInitiator). Connecting...")
                    connect(target, groupOwnerIntent = if (isInitiator) 14 else 1)
                    val connectStartTime = System.currentTimeMillis()
                    val remainingTime = (timeoutMs - (connectStartTime - discoveryStartTime)).coerceAtLeast(35000L)
                    while (System.currentTimeMillis() - connectStartTime < remainingTime) {
                        if (isConnected()) {
                            MeshLogger.i("AUDIO_WIFI_CONNECTED", "[AUDIO_WIFI_CONNECTED] ensureConnected successfully connected to $target")
                            return@withLock true
                        }
                        if (_p2pState.value is WifiP2pState.Error) {
                            MeshLogger.w(TAG, "ensureConnected: Connect attempt failed (${_p2pState.value}), cancelling connect and backing off 2s...")
                            failedConnectAttempts[target] = System.currentTimeMillis()
                            cancelConnect()
                            kotlinx.coroutines.delay(2000L)
                            _p2pState.value = WifiP2pState.Discovering
                            discoverPeers()
                            break
                        }
                        kotlinx.coroutines.delay(200)
                    }
                    if (isConnected()) {
                        MeshLogger.i("AUDIO_WIFI_CONNECTED", "[AUDIO_WIFI_CONNECTED] ensureConnected successfully connected to $target")
                        return@withLock true
                    }
                }

                kotlinx.coroutines.delay(250)
            }

            val connected = isConnected()
            if (connected) {
                MeshLogger.i("AUDIO_WIFI_CONNECTED", "[AUDIO_WIFI_CONNECTED] ensureConnected established link for target=$canonicalTarget")
            }
            return@withLock connected
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, groupOwnerIntent: Int = DEFAULT_GO_INTENT) {
        if (wifiP2pManager == null || channel == null) return
        if (!permissionHandler.hasPermissions()) return

        // Prevent duplicate connection attempts
        val currentState = _p2pState.value
        if (currentState is WifiP2pState.Connected) {
            MeshLogger.d(TAG, "Already connected to Wi-Fi P2P. Reusing existing link.")
            return
        }
        if (currentState is WifiP2pState.Connecting && currentState.deviceAddress == deviceAddress) {
            MeshLogger.d(TAG, "Already connecting to peer: $deviceAddress. Skipping duplicate request.")
            return
        }

        MeshLogger.i("AUDIO_WIFI_CONNECTING", "[AUDIO_WIFI_CONNECTING] Connecting to Wi-Fi Direct peer: $deviceAddress with GO intent $groupOwnerIntent")
        _p2pState.value = WifiP2pState.Connecting(deviceAddress)
        lastConnectedDeviceAddress = deviceAddress
        isAutoReconnectEnabled = true

        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            this.groupOwnerIntent = groupOwnerIntent
            this.wps.setup = WpsInfo.PBC
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
                val reason = getReasonString(reasonCode)
                if (reasonCode == WifiP2pManager.BUSY) {
                    MeshLogger.d(TAG, "Connect initiation returned BUSY for $deviceAddress (ongoing negotiation in progress). Maintaining connection state.")
                    return
                }
                failedConnectAttempts[deviceAddress] = System.currentTimeMillis()
                _discoveredPeers.value = _discoveredPeers.value.filter { it.deviceAddress != deviceAddress }
                connectionTimeoutJob?.cancel()
                MeshLogger.e(TAG, "Connect initiation failed: $reason ($reasonCode)")
                _p2pState.value = WifiP2pState.Error("Connection failed: $reason")
            }
        })
    }

    fun cancelConnect() {
        if (wifiP2pManager == null || channel == null) return
        wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Connect attempt cancelled successfully")
                if (_p2pState.value is WifiP2pState.Connecting) {
                    _p2pState.value = WifiP2pState.Enabled
                }
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.w(TAG, "Cancel connect failed: ${getReasonString(reasonCode)}")
                if (_p2pState.value is WifiP2pState.Connecting) {
                    _p2pState.value = WifiP2pState.Enabled
                }
            }
        })
    }

    fun disconnect() {
        if (wifiP2pManager == null || channel == null) return

        MeshLogger.d(TAG, "Disconnect requested")
        isAutoReconnectEnabled = false
        connectionTimeoutJob?.cancel()
        reconnectJob?.cancel()
        reconnectAttemptCount = 0
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
                _connectedClientsCount.value = group.clientList.size
            } else {
                _connectedClientsCount.value = 0
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
            _connectedClientsCount.value = 0
        }
    }

    fun onPeersChanged() {
        requestPeers()
    }

    fun onDiscoveryChanged(isDiscovering: Boolean) {
        MeshLogger.d(TAG, "onDiscoveryChanged: isDiscovering=$isDiscovering, currentState=${_p2pState.value}")
        if (isDiscovering) {
            if (_p2pState.value !is WifiP2pState.Connected && _p2pState.value !is WifiP2pState.Connecting) {
                _p2pState.value = WifiP2pState.Discovering
            }
        } else {
            if (_p2pState.value is WifiP2pState.Discovering) {
                _p2pState.value = WifiP2pState.Enabled
            }
        }
    }

    fun onConnectionChanged(networkInfo: NetworkInfo?) {
        if (networkInfo?.isConnected == true) {
            requestConnectionInfo()
            requestGroupInfo()
        } else {
            val previousState = _p2pState.value
            if (previousState is WifiP2pState.Connecting) {
                if (networkInfo?.detailedState == NetworkInfo.DetailedState.FAILED) {
                    MeshLogger.w(TAG, "onConnectionChanged: Group formation failed (DetailedState.FAILED)")
                    connectionTimeoutJob?.cancel()
                    _p2pState.value = WifiP2pState.Error("Group creation failed")
                } else {
                    MeshLogger.d(TAG, "onConnectionChanged: isConnected=false with state ${networkInfo?.detailedState} while Connecting. Maintaining Connecting state during group formation.")
                }
                return
            }

            connectionTimeoutJob?.cancel()
            _connectedClientsCount.value = 0
            _p2pState.value = WifiP2pState.Disconnected

            if (previousState is WifiP2pState.Connected && isAutoReconnectEnabled && lastConnectedDeviceAddress != null) {
                MeshLogger.d(TAG, "Link dropped. Triggering automatic reconnect attempt to $lastConnectedDeviceAddress")
                triggerAutoReconnect(lastConnectedDeviceAddress!!)
            }
        }
    }

    fun onThisDeviceChanged(device: WifiP2pDevice?) {
        if (device != null) {
            if (!device.deviceName.isNullOrBlank()) {
                _localDeviceName.value = device.deviceName
            }
            if (!device.deviceAddress.isNullOrBlank() && !device.deviceAddress.contains("02:00:00:00:00:00", ignoreCase = true)) {
                _localDeviceAddress.value = device.deviceAddress
            }
            MeshLogger.d(TAG, "Local device details: name=${_localDeviceName.value}, address=${_localDeviceAddress.value}, status=${device.status}")
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

        val cleanedList = if (updatedList.isNotEmpty()) {
            updatedList
        } else {
            _discoveredPeers.value.filter { it.status == WifiP2pDevice.CONNECTED }
        }

        _discoveredPeers.value = cleanedList
        MeshLogger.i("AUDIO_WIFI_DISCOVERY", "[AUDIO_WIFI_DISCOVERY] Discovered ${cleanedList.size} Wi-Fi Direct peers: ${cleanedList.map { "${it.deviceName}(${it.deviceAddress}, status=${it.statusString})" }}")
    }

    private fun handleConnectionInfo(info: WifiP2pInfo?) {
        connectionTimeoutJob?.cancel()

        if (info != null && info.groupFormed) {
            reconnectJob?.cancel()
            reconnectAttemptCount = 0
            val goAddress = info.groupOwnerAddress?.hostAddress ?: ""
            val isGo = info.isGroupOwner
            MeshLogger.i("AUDIO_WIFI_CONNECTED", "[AUDIO_WIFI_CONNECTED] P2P group formed: groupOwnerAddress=$goAddress, isGroupOwner=$isGo")
            _p2pState.value = WifiP2pState.Connected(groupOwnerAddress = goAddress, isGroupOwner = isGo)
            if (!isGo) {
                _connectedClientsCount.value = 1
            } else {
                requestGroupInfo()
            }
        } else {
            MeshLogger.d(TAG, "Group not formed or connection info null")
            _connectedClientsCount.value = 0
        }
    }

    private fun triggerAutoReconnect(targetAddress: String) {
        if (reconnectAttemptCount >= MAX_RECONNECT_ATTEMPTS) {
            MeshLogger.w(TAG, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached for $targetAddress. Halting reconnect loop.")
            _p2pState.value = WifiP2pState.Disconnected
            return
        }

        reconnectJob?.cancel()
        reconnectJob = applicationScope.launch {
            reconnectAttemptCount++
            val delayMs = (INITIAL_RECONNECT_DELAY_MS * (1 shl (reconnectAttemptCount - 1))).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            
            _p2pState.value = WifiP2pState.Recovering
            MeshLogger.d(TAG, "Attempting auto-reconnect ($reconnectAttemptCount/$MAX_RECONNECT_ATTEMPTS) to $targetAddress with backoff ${delayMs}ms...")
            delay(delayMs)
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
