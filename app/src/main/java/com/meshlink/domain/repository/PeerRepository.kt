package com.meshlink.domain.repository

import com.meshlink.domain.model.PeerInfo

/**
 * Manages peer data storage and retrieval.
 *
 * Responsibility: Provide access to known peers in the network.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe.
 */
interface PeerRepository {
    suspend fun getPeers(): List<PeerInfo>
    suspend fun getPeer(peerId: String): PeerInfo?
    suspend fun updatePeer(peerInfo: PeerInfo)
}
