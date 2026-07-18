package com.meshlink.domain.model

data class MeshStatus(
    val isBleAdvertising: Boolean,
    val isBleScanning: Boolean,
    val connectedPeersCount: Int,
    val isServerRunning: Boolean
)
