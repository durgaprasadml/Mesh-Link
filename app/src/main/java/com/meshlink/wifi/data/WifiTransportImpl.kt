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

    override suspend fun send(packet: MeshPacket) {
        wifiSocketTransport.sendPacket(packet)
    }

    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        // Wi-Fi socket is typically point-to-point in this implementation
        wifiSocketTransport.sendPacket(packet)
    }

    override suspend fun connect(peerId: String) {
        wifiSocketTransport.connectAsClient(peerId)
    }
}
