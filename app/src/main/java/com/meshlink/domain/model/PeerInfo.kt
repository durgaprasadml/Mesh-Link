package com.meshlink.domain.model

data class PeerInfo(
    val peerId: String,
    val name: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val connectionState: PeerConnectionState = PeerConnectionState.DISCONNECTED
)
