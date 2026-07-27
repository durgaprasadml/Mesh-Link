package com.meshlink.domain.transport

import com.meshlink.domain.model.MeshPacket

/**
 * Transport abstraction representing a communication medium (e.g., BLE, Wi-Fi).
 *
 * Responsibility: Provide a unified interface for sending and broadcasting packets.
 * Lifecycle Ownership: Application scoped.
 * Dependencies: Domain models.
 */
interface Transport {
    val incomingPackets: kotlinx.coroutines.flow.SharedFlow<Pair<String, MeshPacket>>
    val connectedPeers: Set<String>

    suspend fun send(packet: MeshPacket)
    suspend fun broadcast(packet: MeshPacket, excludeAddress: String? = null, includeAddress: String? = null)
    suspend fun connect(peerId: String)
}
