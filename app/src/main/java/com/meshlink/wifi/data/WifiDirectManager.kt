package com.meshlink.wifi.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.os.Looper
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.NetworkInterface

enum class WifiP2pConnectionState {
    DISCONNECTED,
    SEARCHING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class WifiDirectDeviceDetails(
    val deviceName: String,
    val deviceAddress: String,
    val primaryDeviceType: String = "",
    val status: Int = WifiP2pDevice.UNAVAILABLE,
    val isGroupOwner: Boolean = false
)

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "WifiDirectManager"
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_RETRY_DELAY_MS = 2000L
    }

    private val wifiP2pManager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private var channel: WifiP2pManager.Channel? = null

    private val _isP2pEnabled = MutableStateFlow(false)
    val isP2pEnabled: StateFlow<Boolean> = _isP2pEnabled.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers.asStateFlow()

    private val _connectionState = MutableStateFlow(WifiP2pConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WifiP2pConnectionState> = _connectionState.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _groupInfo = MutableStateFlow<WifiP2pGroup?>(null)
    val groupInfo: StateFlow<WifiP2pGroup?> = _groupInfo.asStateFlow()

    private val _isGroupOwner = MutableStateFlow(false)
    val isGroupOwner: StateFlow<Boolean> = _isGroupOwner.asStateFlow()

    private val _groupOwnerAddress = MutableStateFlow<String?>(null)
    val groupOwnerAddress: StateFlow<String?> = _groupOwnerAddress.asStateFlow()

    private val _localIpAddress = MutableStateFlow<String?>(null)
    val localIpAddress: StateFlow<String?> = _localIpAddress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var discoveryRetryJob: Job? = null
    private var retryCount = 0

    init {
        initializeChannel()
    }

    fun initializeChannel() {
        if (channel != null) return
        val manager = wifiP2pManager ?: run {
            MeshLogger.e(TAG, "WifiP2pManager system service is unavailable")
            _connectionState.value = WifiP2pConnectionState.ERROR
            return
        }

        channel = manager.initialize(context, Looper.getMainLooper()) {
            MeshLogger.w(TAG, "Wi-Fi Direct channel lost. Attempting re-initialization...")
            channel = null
            _connectionState.value = WifiP2pConnectionState.DISCONNECTED
            applicationScope.launch {
                delay(1000)
                initializeChannel()
            }
        }
        MeshLogger.d(TAG, "Wi-Fi Direct channel successfully initialized")
    }

    fun setP2pEnabled(enabled: Boolean) {
        _isP2pEnabled.value = enabled
        MeshLogger.d(TAG, "Wi-Fi P2P state updated: enabled=$enabled")
        if (!enabled) {
            _connectionState.value = WifiP2pConnectionState.DISCONNECTED
            _peers.value = emptyList()
            _isDiscovering.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(onResult: ((Boolean) -> Unit)? = null) {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: run {
            initializeChannel()
            channel ?: return
        }

        if (_isDiscovering.value) {
            onResult?.invoke(true)
            return
        }

        _isDiscovering.value = true
        _connectionState.value = if (_connectionState.value == WifiP2pConnectionState.DISCONNECTED) {
            WifiP2pConnectionState.SEARCHING
        } else {
            _connectionState.value
        }

        manager.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Peer discovery initiated successfully")
                retryCount = 0
                onResult?.invoke(true)
            }

            override fun onFailure(reasonCode: Int) {
                _isDiscovering.value = false
                val reason = getReasonString(reasonCode)
                MeshLogger.w(TAG, "Peer discovery failed: $reason (code: $reasonCode)")
                _errorMessage.value = "Discovery failed: $reason"
                onResult?.invoke(false)

                // Backoff retry logic
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++
                    val backoffDelay = BASE_RETRY_DELAY_MS * (1 shl (retryCount - 1))
                    discoveryRetryJob?.cancel()
                    discoveryRetryJob = applicationScope.launch {
                        delay(backoffDelay)
                        MeshLogger.d(TAG, "Retrying peer discovery (Attempt $retryCount)")
                        discoverPeers()
                    }
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        manager.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _isDiscovering.value = false
                MeshLogger.d(TAG, "Peer discovery stopped")
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.w(TAG, "Failed to stop peer discovery: ${getReasonString(reasonCode)}")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        manager.requestPeers(ch) { peerList ->
            val deviceList = peerList.deviceList.toList()
            _peers.value = deviceList
            MeshLogger.d(TAG, "Updated peer list: ${deviceList.size} device(s) found")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, groupOwnerIntent: Int = -1, onResult: ((Boolean) -> Unit)? = null) {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            if (groupOwnerIntent in 0..15) {
                this.groupOwnerIntent = groupOwnerIntent
            }
        }

        _connectionState.value = WifiP2pConnectionState.CONNECTING
        MeshLogger.d(TAG, "Initiating connection to $deviceAddress...")

        manager.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Connect request accepted for $deviceAddress")
                onResult?.invoke(true)
            }

            override fun onFailure(reasonCode: Int) {
                val reason = getReasonString(reasonCode)
                MeshLogger.e(TAG, "Connection to $deviceAddress failed: $reason")
                _connectionState.value = WifiP2pConnectionState.ERROR
                _errorMessage.value = "Connection failed: $reason"
                onResult?.invoke(false)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun createGroup(onResult: ((Boolean) -> Unit)? = null) {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        MeshLogger.d(TAG, "Creating autonomous Wi-Fi Direct group...")
        manager.createGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Wi-Fi Direct group created successfully")
                onResult?.invoke(true)
            }

            override fun onFailure(reasonCode: Int) {
                val reason = getReasonString(reasonCode)
                MeshLogger.e(TAG, "Failed to create group: $reason")
                _errorMessage.value = "Create group failed: $reason"
                onResult?.invoke(false)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun removeGroup(onResult: ((Boolean) -> Unit)? = null) {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        manager.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                MeshLogger.d(TAG, "Wi-Fi Direct group removed")
                _connectionState.value = WifiP2pConnectionState.DISCONNECTED
                _connectionInfo.value = null
                _groupInfo.value = null
                _isGroupOwner.value = false
                _groupOwnerAddress.value = null
                onResult?.invoke(true)
            }

            override fun onFailure(reasonCode: Int) {
                MeshLogger.w(TAG, "Failed to remove group: ${getReasonString(reasonCode)}")
                onResult?.invoke(false)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect(onResult: ((Boolean) -> Unit)? = null) {
        removeGroup(onResult)
    }

    @SuppressLint("MissingPermission")
    fun requestConnectionInfo() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        manager.requestConnectionInfo(ch) { info ->
            _connectionInfo.value = info
            if (info != null && info.groupFormed) {
                _connectionState.value = WifiP2pConnectionState.CONNECTED
                _isGroupOwner.value = info.isGroupOwner
                _groupOwnerAddress.value = info.groupOwnerAddress?.hostAddress

                if (info.isGroupOwner) {
                    _localIpAddress.value = info.groupOwnerAddress?.hostAddress
                } else {
                    _localIpAddress.value = getLocalP2pIpAddress()
                }

                MeshLogger.d(TAG, "Connection Info updated: groupFormed=true, isGO=${info.isGroupOwner}, GO IP=${_groupOwnerAddress.value}, Local IP=${_localIpAddress.value}")
                requestGroupInfo()
            } else {
                _connectionState.value = WifiP2pConnectionState.DISCONNECTED
                _isGroupOwner.value = false
                _groupOwnerAddress.value = null
                _localIpAddress.value = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGroupInfo() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        manager.requestGroupInfo(ch) { group ->
            _groupInfo.value = group
            if (group != null) {
                MeshLogger.d(TAG, "Group Info: networkName=${group.networkName}, passhrase=${group.passphrase}, clientCount=${group.clientList.size}")
            }
        }
    }

    fun onNetworkStateChanged(networkInfo: NetworkInfo?) {
        if (networkInfo?.isConnected == true) {
            MeshLogger.d(TAG, "NetworkStateChanged: Connected. Requesting connection info...")
            _connectionState.value = WifiP2pConnectionState.CONNECTING
            requestConnectionInfo()
        } else {
            MeshLogger.d(TAG, "NetworkStateChanged: Disconnected")
            _connectionState.value = WifiP2pConnectionState.DISCONNECTED
            _connectionInfo.value = null
            _groupInfo.value = null
            _isGroupOwner.value = false
            _groupOwnerAddress.value = null
            _localIpAddress.value = null
        }
    }

    private fun getLocalP2pIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.name.contains("p2p") || intf.name.contains("wlan")) {
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to get local P2P IP address: ${e.message}")
        }
        return null
    }

    private fun getReasonString(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.ERROR -> "Internal Error"
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported on this device"
            WifiP2pManager.BUSY -> "Framework Busy"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "No Service Requests"
            else -> "Unknown Error ($reasonCode)"
        }
    }

    fun cleanup() {
        discoveryRetryJob?.cancel()
        removeGroup()
        _peers.value = emptyList()
        _isDiscovering.value = false
    }
}
