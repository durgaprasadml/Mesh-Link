package com.meshlink.ble.data

import com.meshlink.common.util.MeshPacketParser

import com.meshlink.ble.api.BleTransport
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.transport.Transport
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import com.meshlink.di.ApplicationScope

@Singleton
internal class BleTransportImpl @Inject constructor(
    private val gattManager: BleGattManager,
    private val connectionManager: BleConnectionManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) : BleTransport {

    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = gattManager.incomingMessages
        .mapNotNull { (sender, json) ->
            val packet = MeshPacketParser.fromJson(json)
            if (packet != null) sender to packet else null
        }
        .shareIn(applicationScope, SharingStarted.Eagerly, 200)

    override val connectedPeers: Set<String>
        get() = gattManager.connectedServers.keys + gattManager.activeClients.keys

    override suspend fun send(packet: MeshPacket) {
        val json = MeshPacketParser.toJson(packet)
        gattManager.broadcastPacket(json, includeAddress = packet.targetId)
    }

    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        val json = MeshPacketParser.toJson(packet)
        gattManager.broadcastPacket(json, excludeAddress = excludeAddress, includeAddress = includeAddress)
    }

    override suspend fun connect(peerId: String) {
        connectionManager.connectToDevice(peerId)
    }
}
