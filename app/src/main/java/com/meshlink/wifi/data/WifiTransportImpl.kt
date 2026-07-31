package com.meshlink.wifi.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshError
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.wifi.api.WifiTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@Singleton
internal class WifiTransportImpl @Inject constructor(
    private val wifiSocketTransport: WifiSocketTransport,
    private val wifiDirectManager: WifiDirectManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) : WifiTransport {

    companion object {
        private const val TAG = "WifiTransportImpl"
    }

    private val _incomingPackets = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 200)
    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = _incomingPackets.asSharedFlow()

    override val isP2pEnabled: Boolean
        get() = wifiDirectManager.isP2pEnabled.value

    override val isConnected: Boolean
        get() = wifiSocketTransport.isConnected()

    override val connectedPeers: Set<String>
        get() = wifiSocketTransport.connectedPeers

    init {
        // Wire incoming socket packets to flow
        wifiSocketTransport.onPacketReceived = { senderAddress, packet ->
            applicationScope.launch {
                MeshLogger.d(TAG, "Wi-Fi packet received from $senderAddress: type=${packet.type}, id=${packet.packetId}")
                _incomingPackets.emit(senderAddress to packet)
            }
        }

        // Handle Wi-Fi connection info state -> auto start/connect sockets
        applicationScope.launch {
            wifiDirectManager.connectionInfo.collect { info ->
                if (info != null && info.groupFormed) {
                    if (info.isGroupOwner) {
                        MeshLogger.d(TAG, "Device is Group Owner. Starting socket server...")
                        wifiSocketTransport.startServer()
                    } else {
                        val goIp = info.groupOwnerAddress?.hostAddress
                        if (!goIp.isNullOrBlank()) {
                            MeshLogger.d(TAG, "Device is Client. Connecting to GO at $goIp...")
                            wifiSocketTransport.connectAsClient(goIp)
                        }
                    }
                } else {
                    MeshLogger.d(TAG, "Group dissolved. Stopping socket server and closing connections.")
                    wifiSocketTransport.stopServer()
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
            val success = wifiSocketTransport.sendPacket(packet)
            if (success) {
                MeshResult.Success(Unit)
            } else {
                MeshResult.Error(MeshError.TransportError("Failed to send Wi-Fi packet to ${packet.targetId}"))
            }
        } catch (e: Exception) {
            MeshResult.Error(
                MeshError.TransportError("Failed to send Wi-Fi packet", cause = e)
            )
        }
    }

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        wifiSocketTransport.broadcastPacket(packet, excludeAddress, includeAddress)
    }

    override suspend fun broadcastPacket(packet: MeshPacket, excludeAddress: String?, includeAddress: String?): MeshResult<Unit> {
        return try {
            wifiSocketTransport.broadcastPacket(packet, excludeAddress, includeAddress)
            MeshResult.Success(Unit)
        } catch (e: Exception) {
            MeshResult.Error(
                MeshError.TransportError("Failed to broadcast Wi-Fi packet", cause = e)
            )
        }
    }

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) {
        wifiDirectManager.connect(peerId)
    }

    override suspend fun connectToPeer(peerId: String): MeshResult<Unit> {
        return try {
            wifiDirectManager.connect(peerId)
            MeshResult.Success(Unit)
        } catch (e: Exception) {
            MeshResult.Error(
                MeshError.TransportError("Failed to connect via Wi-Fi Direct", deviceAddress = peerId, cause = e)
            )
        }
    }
}
