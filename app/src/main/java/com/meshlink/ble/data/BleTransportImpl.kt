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

import com.meshlink.domain.transport.TransportHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class BleTransportImpl @Inject constructor(
    private val gattManager: BleGattManager,
    private val connectionManager: BleConnectionManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) : BleTransport {

    private val _healthState = MutableStateFlow(TransportHealth.CONNECTED)
    override val health: StateFlow<TransportHealth> = _healthState.asStateFlow()

    private val _connectedPeersFlow = MutableStateFlow<Set<String>>(emptySet())
    override val connectedPeersFlow: StateFlow<Set<String>>
        get() {
            _connectedPeersFlow.value = connectedPeers
            return _connectedPeersFlow.asStateFlow()
        }

    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = gattManager.incomingMessages
        .mapNotNull { (sender, json) ->
            val packet = MeshPacketParser.fromJson(json)
            if (packet != null) sender to packet else null
        }
        .shareIn(applicationScope, SharingStarted.Eagerly, 200)

    override val connectedPeers: Set<String>
        get() {
            val peers = gattManager.connectedServers.keys + gattManager.activeClients.keys
            _connectedPeersFlow.value = peers
            _healthState.value = if (peers.isNotEmpty()) TransportHealth.CONNECTED else TransportHealth.AVAILABLE
            return peers
        }

    @Deprecated("Use sendPacket instead", ReplaceWith("sendPacket(packet)"))
    override suspend fun send(packet: MeshPacket) {
        val json = MeshPacketParser.toJson(packet)
        gattManager.broadcastPacket(json, includeAddress = packet.targetId)
    }

    override suspend fun sendPacket(packet: MeshPacket): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            val json = MeshPacketParser.toJson(packet)
            gattManager.broadcastPacket(json, includeAddress = packet.targetId)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(
                com.meshlink.domain.model.MeshError.TransportError("Failed to send BLE packet", cause = e)
            )
        }
    }

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        val json = MeshPacketParser.toJson(packet)
        gattManager.broadcastPacket(json, excludeAddress = excludeAddress, includeAddress = includeAddress)
    }

    override suspend fun broadcastPacket(packet: MeshPacket, excludeAddress: String?, includeAddress: String?): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            val json = MeshPacketParser.toJson(packet)
            gattManager.broadcastPacket(json, excludeAddress = excludeAddress, includeAddress = includeAddress)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(
                com.meshlink.domain.model.MeshError.TransportError("Failed to broadcast BLE packet", cause = e)
            )
        }
    }

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) {
        connectionManager.connectToDevice(peerId)
    }

    override suspend fun connectToPeer(peerId: String): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            connectionManager.connectToDevice(peerId)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(
                com.meshlink.domain.model.MeshError.TransportError("Failed to connect via BLE", deviceAddress = peerId, cause = e)
            )
        }
    }
}
