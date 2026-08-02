package com.meshlink.wifi.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshError
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.transport.Transport
import com.meshlink.wifi.manager.WifiP2pManagerFacade
import com.meshlink.wifi.model.WifiP2pState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import com.meshlink.domain.transport.TransportHealth
import com.meshlink.wifi.api.WifiTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class WifiTransportImpl @Inject constructor(
    private val wifiP2pManagerFacade: WifiP2pManagerFacade,
    private val wifiSocketTransport: WifiSocketTransport,
    @ApplicationScope private val applicationScope: CoroutineScope
) : WifiTransport {

    companion object {
        private const val TAG = "WifiTransportImpl"
    }

    private val _healthState = MutableStateFlow(TransportHealth.DISCONNECTED)
    override val health: StateFlow<TransportHealth> = _healthState.asStateFlow()

    private val _connectedPeersFlow = MutableStateFlow<Set<String>>(emptySet())
    override val connectedPeersFlow: StateFlow<Set<String>>
        get() {
            _connectedPeersFlow.value = connectedPeers
            return _connectedPeersFlow.asStateFlow()
        }

    private val _incomingPackets = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 100)
    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = _incomingPackets.asSharedFlow()

    override val connectedPeers: Set<String>
        get() {
            val peers = if (wifiSocketTransport.isConnected()) {
                val p2pConnected = wifiP2pManagerFacade.discoveredPeers.value
                    .filter { it.status == android.net.wifi.p2p.WifiP2pDevice.CONNECTED }
                    .map { it.deviceAddress }
                    .toSet()
                if (p2pConnected.isNotEmpty()) p2pConnected else setOf("WIFI_DIRECT_PEER")
            } else {
                emptySet()
            }
            _connectedPeersFlow.value = peers
            if (peers.isNotEmpty() && wifiSocketTransport.isConnected()) {
                _healthState.value = TransportHealth.CONNECTED
            } else if (_healthState.value == TransportHealth.CONNECTED) {
                _healthState.value = TransportHealth.DISCONNECTED
            }
            return peers
        }

    init {
        // Handle incoming packets from socket layer
        wifiSocketTransport.onPacketReceived = { packet ->
            applicationScope.launch {
                MeshLogger.d(TAG, "Incoming packet received over Wi-Fi Direct from ${packet.senderId}")
                _incomingPackets.emit(packet.senderId to packet)
            }
        }

        // Monitor P2P State transitions to automatically orchestrate socket layer
        applicationScope.launch {
            wifiP2pManagerFacade.p2pState.collect { state ->
                MeshLogger.d(TAG, "P2P State updated: $state")
                when (state) {
                    is WifiP2pState.Connected -> {
                        _healthState.value = TransportHealth.CONNECTING
                        if (state.isGroupOwner) {
                            MeshLogger.d(TAG, "Starting ServerSocket as Group Owner...")
                            wifiSocketTransport.startServer()
                        } else {
                            MeshLogger.d(TAG, "Connecting as Client to Group Owner at ${state.groupOwnerAddress}...")
                            wifiSocketTransport.connectAsClient(state.groupOwnerAddress)
                        }
                    }

                    is WifiP2pState.Disconnected -> {
                        _healthState.value = TransportHealth.DISCONNECTED
                        MeshLogger.d(TAG, "Wi-Fi Direct disconnected. Stopping socket streams...")
                        wifiSocketTransport.disconnect()
                    }

                    else -> {
                        _healthState.value = TransportHealth.AVAILABLE
                    }
                }
            }
        }
    }

    @Deprecated("Use sendPacket instead", ReplaceWith("sendPacket(packet)"))
    override suspend fun send(packet: MeshPacket) {
        wifiSocketTransport.sendPacket(packet)
    }

    override suspend fun sendPacket(packet: MeshPacket): MeshResult<Unit> {
        return try {
            wifiSocketTransport.sendPacket(packet)
            MeshResult.Success(Unit)
        } catch (e: Exception) {
            MeshResult.Error(
                MeshError.TransportError("Failed to send Wi-Fi packet", cause = e)
            )
        }
    }

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        wifiSocketTransport.sendPacket(packet)
    }

    override suspend fun broadcastPacket(packet: MeshPacket, excludeAddress: String?, includeAddress: String?): MeshResult<Unit> {
        return try {
            wifiSocketTransport.sendPacket(packet)
            MeshResult.Success(Unit)
        } catch (e: Exception) {
            MeshResult.Error(
                MeshError.TransportError("Failed to broadcast Wi-Fi packet", cause = e)
            )
        }
    }

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) {
        wifiP2pManagerFacade.connect(peerId)
    }

    override suspend fun connectToPeer(peerId: String): MeshResult<Unit> {
        return try {
            wifiP2pManagerFacade.connect(peerId)
            MeshResult.Success(Unit)
        } catch (e: Exception) {
            MeshResult.Error(
                MeshError.TransportError("Failed to connect via Wi-Fi P2P", deviceAddress = peerId, cause = e)
            )
        }
    }
}
