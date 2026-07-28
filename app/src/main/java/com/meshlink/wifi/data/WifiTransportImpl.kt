package com.meshlink.wifi.data

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.transport.Transport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.meshlink.di.ApplicationScope

@Singleton
internal class WifiTransportImpl @Inject constructor(
    private val wifiSocketTransport: WifiSocketTransport,
    @ApplicationScope private val applicationScope: CoroutineScope
) : Transport {

    private val _incomingPackets = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 100)
    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = _incomingPackets.asSharedFlow()

    override val connectedPeers: Set<String>
        get() = if (wifiSocketTransport.isConnected()) setOf("WIFI_PEER") else emptySet() // Hardcoded for socket representation

    init {
        wifiSocketTransport.onPacketReceived = { packet ->
            applicationScope.launch {
                _incomingPackets.emit("WIFI_PEER" to packet)
            }
        }
    }

    @Deprecated("Use sendPacket instead", ReplaceWith("sendPacket(packet)"))
    override suspend fun send(packet: MeshPacket) {
        wifiSocketTransport.sendPacket(packet)
    }

    override suspend fun sendPacket(packet: MeshPacket): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            wifiSocketTransport.sendPacket(packet)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(
                com.meshlink.domain.model.MeshError.TransportError("Failed to send Wi-Fi packet", cause = e)
            )
        }
    }

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        // Wi-Fi socket is typically point-to-point in this implementation
        wifiSocketTransport.sendPacket(packet)
    }

    override suspend fun broadcastPacket(packet: MeshPacket, excludeAddress: String?, includeAddress: String?): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            wifiSocketTransport.sendPacket(packet)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(
                com.meshlink.domain.model.MeshError.TransportError("Failed to broadcast Wi-Fi packet", cause = e)
            )
        }
    }

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) {
        wifiSocketTransport.connectAsClient(peerId)
    }

    override suspend fun connectToPeer(peerId: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            wifiSocketTransport.connectAsClient(peerId)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(
                com.meshlink.domain.model.MeshError.TransportError("Failed to connect via Wi-Fi", deviceAddress = peerId, cause = e)
            )
        }
    }
}
