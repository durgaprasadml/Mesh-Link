package com.meshlink.messaging.api

import com.meshlink.domain.model.MeshPacket

interface MessageProcessor {
    suspend fun processIncomingPacket(packet: MeshPacket)
    suspend fun sendOutgoingMessage(destinationId: String, payload: String)
}
