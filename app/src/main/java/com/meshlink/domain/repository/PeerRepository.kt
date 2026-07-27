package com.meshlink.domain.repository

import com.meshlink.domain.model.PeerInfo

interface PeerRepository {
    suspend fun getPeers(): List<PeerInfo>
    suspend fun getPeer(peerId: String): PeerInfo?
    suspend fun updatePeer(peerInfo: PeerInfo)
}
