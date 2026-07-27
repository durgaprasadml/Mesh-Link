package com.meshlink.routing.api

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import kotlinx.coroutines.flow.SharedFlow

interface Router {
    var localMeshId: String
    val incomingPayloads: SharedFlow<Pair<String, MeshPacket>>
    val routeTable: Map<String, RouteEntry>

    fun sendPayload(
        targetId: String,
        payload: String,
        myAddressAlias: String = "Me",
        encrypted: Boolean = false,
        packetId: String? = null
    )

    fun sendMediaPacket(packet: MeshPacket)
}
