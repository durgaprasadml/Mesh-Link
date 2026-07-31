package com.meshlink.wifi.data

import com.meshlink.ble.api.BleTransport
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.*
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.wifi.api.HybridMode
import com.meshlink.wifi.api.HybridTransport
import com.meshlink.wifi.api.WifiTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Singleton
internal class HybridTransportManager @Inject constructor(
    private val bleTransport: BleTransport,
    private val wifiTransport: WifiTransport,
    private val wifiDirectManager: WifiDirectManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : HybridTransport {

    companion object {
        private const val TAG = "HybridTransportManager"
        private const val HIGH_BANDWIDTH_SIZE_BYTES = 1024L // 1 KB threshold
    }

    private val _incomingPackets = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 400)
    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = _incomingPackets.asSharedFlow()

    private val _activeMode = MutableStateFlow(HybridMode.BLE_ONLY)
    override val activeMode: StateFlow<HybridMode> = _activeMode.asStateFlow()

    override val isWifiConnected: Boolean
        get() = wifiTransport.isConnected

    override val isBleConnected: Boolean
        get() = bleTransport.connectedPeers.isNotEmpty()

    override val connectedPeers: Set<String>
        get() = bleTransport.connectedPeers + wifiTransport.connectedPeers

    private val preferredTransportMode = settingsRepository.preferredTransport
        .stateIn(applicationScope, SharingStarted.Eagerly, "AUTOMATIC")

    init {
        // Merge incoming streams from BLE and Wi-Fi Direct seamlessly into one stream
        applicationScope.launch {
            bleTransport.incomingPackets.collect { (sender, packet) ->
                MeshLogger.d(TAG, "Incoming BLE packet from $sender (id=${packet.packetId})")
                _incomingPackets.emit(sender to packet)
            }
        }

        applicationScope.launch {
            wifiTransport.incomingPackets.collect { (sender, packet) ->
                MeshLogger.d(TAG, "Incoming Wi-Fi packet from $sender (id=${packet.packetId})")
                _incomingPackets.emit(sender to packet)
            }
        }

        // Monitor active connection states to update HybridMode
        applicationScope.launch {
            combine(
                wifiDirectManager.connectionState,
                wifiTransport.incomingPackets.map { true }.onStart { emit(false) }
            ) { connState, _ ->
                val wifiConnected = wifiTransport.isConnected || connState == WifiP2pConnectionState.CONNECTED
                val bleConnected = bleTransport.connectedPeers.isNotEmpty()

                when {
                    wifiConnected && bleConnected -> HybridMode.HYBRID_ACTIVE
                    wifiConnected -> HybridMode.WIFI_DIRECT_ONLY
                    else -> HybridMode.BLE_ONLY
                }
            }.collect { mode ->
                _activeMode.value = mode
                MeshLogger.d(TAG, "Hybrid Transport Mode updated to: $mode")
            }
        }
    }

    override fun getSelectedRouteType(
        targetId: String,
        packetType: PacketType,
        payloadSize: Long
    ): RouteType {
        val pref = preferredTransportMode.value
        if (pref == "BLE_ONLY") return RouteType.BLE
        if (pref == "WIFI_ONLY" && wifiTransport.isConnected) return RouteType.WIFI_DIRECT

        val requiresHighBandwidth = when (packetType) {
            PacketType.VIDEO_FRAME,
            PacketType.VOICE_FRAME,
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_META,
            PacketType.MEDIA_ACK,
            PacketType.MEDIA_NACK -> true
            else -> payloadSize > HIGH_BANDWIDTH_SIZE_BYTES
        }

        // Check if Wi-Fi Direct connection is active for this target or overall
        val isWifiAvailable = wifiTransport.isConnected &&
                (wifiTransport.connectedPeers.contains(targetId) || wifiTransport.connectedPeers.isNotEmpty())

        return if (requiresHighBandwidth && isWifiAvailable) {
            RouteType.WIFI_DIRECT
        } else if (requiresHighBandwidth && wifiDirectManager.isP2pEnabled.value && !wifiTransport.isConnected) {
            // Trigger auto upgrade negotiation in background
            triggerAutoUpgrade(targetId)
            RouteType.BLE // Send current over BLE while negotiating Wi-Fi
        } else {
            RouteType.BLE
        }
    }

    override fun triggerAutoUpgrade(peerAddress: String) {
        if (!wifiDirectManager.isP2pEnabled.value) return
        if (wifiTransport.isConnected || wifiDirectManager.connectionState.value == WifiP2pConnectionState.CONNECTING) return

        applicationScope.launch {
            MeshLogger.d(TAG, "Auto-Upgrade triggered for peer $peerAddress. Negotiating Wi-Fi Direct...")
            wifiDirectManager.connect(peerAddress)
        }
    }

    @Deprecated("Use sendPacket instead", ReplaceWith("sendPacket(packet)"))
    override suspend fun send(packet: MeshPacket) {
        sendPacket(packet)
    }

    override suspend fun sendPacket(packet: MeshPacket): MeshResult<Unit> {
        val payloadSize = packet.payload.length.toLong()
        val selectedRoute = getSelectedRouteType(packet.targetId, packet.type, payloadSize)

        return if (selectedRoute == RouteType.WIFI_DIRECT) {
            MeshLogger.d(TAG, "Routing packet ${packet.packetId} via Wi-Fi Direct")
            val wifiResult = wifiTransport.sendPacket(packet)
            if (wifiResult is MeshResult.Success) {
                wifiResult
            } else {
                // AUTOMATIC DOWNGRADE: Wi-Fi send failed -> fallback to BLE immediately
                MeshLogger.w(TAG, "Wi-Fi send failed for packet ${packet.packetId}. Downgrading to BLE fallback...")
                bleTransport.sendPacket(packet)
            }
        } else {
            MeshLogger.d(TAG, "Routing packet ${packet.packetId} via BLE")
            bleTransport.sendPacket(packet)
        }
    }

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        broadcastPacket(packet, excludeAddress, includeAddress)
    }

    override suspend fun broadcastPacket(
        packet: MeshPacket,
        excludeAddress: String?,
        includeAddress: String?
    ): MeshResult<Unit> {
        val payloadSize = packet.payload.length.toLong()
        val selectedRoute = getSelectedRouteType(includeAddress ?: packet.targetId, packet.type, payloadSize)

        return if (selectedRoute == RouteType.WIFI_DIRECT && wifiTransport.isConnected) {
            MeshLogger.d(TAG, "Broadcasting packet ${packet.packetId} via Wi-Fi Direct")
            val wifiResult = wifiTransport.broadcastPacket(packet, excludeAddress, includeAddress)
            // Send on BLE as well if in HYBRID mode to ensure complete coverage across mesh nodes
            if (_activeMode.value == HybridMode.HYBRID_ACTIVE) {
                bleTransport.broadcastPacket(packet, excludeAddress, includeAddress)
            }
            wifiResult
        } else {
            MeshLogger.d(TAG, "Broadcasting packet ${packet.packetId} via BLE")
            bleTransport.broadcastPacket(packet, excludeAddress, includeAddress)
        }
    }

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) {
        connectToPeer(peerId)
    }

    override suspend fun connectToPeer(peerId: String): MeshResult<Unit> {
        // Connect over BLE first for basic link
        val bleResult = bleTransport.connectToPeer(peerId)

        // Attempt Wi-Fi Direct connection in parallel if P2P enabled
        if (wifiDirectManager.isP2pEnabled.value && !wifiTransport.isConnected) {
            triggerAutoUpgrade(peerId)
        }

        return bleResult
    }
}
