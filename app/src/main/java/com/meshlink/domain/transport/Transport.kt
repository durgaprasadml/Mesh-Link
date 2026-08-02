package com.meshlink.domain.transport

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import kotlinx.coroutines.flow.SharedFlow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport abstraction representing a communication medium (e.g., BLE, Wi-Fi).
 *
 * Responsibility: Provide a unified interface for connecting to peers, sending, and broadcasting packets.
 * Lifecycle Ownership: Application scoped.
 * Thread Safety: Implementations must be thread-safe.
 * Return Contract: Asynchronous operations return `MeshResult`.
 * Failure Conditions: Network failures, timeouts, unavailable devices.
 */
interface Transport {
    val incomingPackets: SharedFlow<Pair<String, MeshPacket>>
    val connectedPeers: Set<String>
    val connectedPeersFlow: StateFlow<Set<String>>
        get() = MutableStateFlow(connectedPeers)
    val health: StateFlow<TransportHealth>
        get() = MutableStateFlow(if (connectedPeers.isNotEmpty()) TransportHealth.CONNECTED else TransportHealth.AVAILABLE)

    @Deprecated("Use sendPacket instead", ReplaceWith("sendPacket(packet)"))
    suspend fun send(packet: MeshPacket)

    /**
     * Sends a packet to a specific connected peer.
     * @param packet The packet to send.
     * @return [MeshResult.Success] on success, [MeshResult.Error] on transport failure.
     */
    suspend fun sendPacket(packet: MeshPacket): MeshResult<Unit>

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    suspend fun broadcast(packet: MeshPacket, excludeAddress: String? = null, includeAddress: String? = null)

    /**
     * Broadcasts a packet to all connected peers, optionally filtering by address.
     * @param packet The packet to broadcast.
     * @param excludeAddress An address to exclude from the broadcast.
     * @param includeAddress An address to exclusively include in the broadcast.
     * @return [MeshResult.Success] on success, [MeshResult.Error] on transport failure.
     */
    suspend fun broadcastPacket(packet: MeshPacket, excludeAddress: String? = null, includeAddress: String? = null): MeshResult<Unit>

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    suspend fun connect(peerId: String)

    /**
     * Attempts to connect to a specific peer over this transport.
     * @param peerId The ID or address of the peer to connect to.
     * @return [MeshResult.Success] on successful connection, [MeshResult.Error] on failure.
     */
    suspend fun connectToPeer(peerId: String): MeshResult<Unit>
}
