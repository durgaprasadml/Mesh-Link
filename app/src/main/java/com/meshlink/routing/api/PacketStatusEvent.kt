package com.meshlink.routing.api

sealed interface PacketStatusEvent {
    val packetId: String
}

data class PacketQueued(override val packetId: String) : PacketStatusEvent
data class PacketTransmissionStarted(override val packetId: String) : PacketStatusEvent
data class PacketTransmitted(override val packetId: String) : PacketStatusEvent
data class PacketFailed(override val packetId: String, val cause: Throwable? = null) : PacketStatusEvent
