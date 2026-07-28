package com.meshlink.routing.api

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.DispatchResult
import com.meshlink.domain.model.RouteEntry
import kotlinx.coroutines.flow.SharedFlow

/**
 * Handles routing of packets across the mesh network.
 *
 * Responsibility: Determine the best path for packets and manage the routing table.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe for concurrent routing.
 * Return Contract: Routing operations return [DispatchResult] to indicate queue admission status.
 * Failure Conditions: Route not found, target unreachable, TTL expired.
 */
interface Router {
    var localMeshId: String
    val incomingPayloads: SharedFlow<Pair<String, MeshPacket>>
    val routeTable: Map<String, RouteEntry>
    val packetEvents: SharedFlow<PacketStatusEvent>

    @Deprecated("Use routePayload instead", ReplaceWith("routePayload(targetId, payload, myAddressAlias, encrypted, packetId)"))
    fun sendPayload(
        targetId: String,
        payload: String,
        myAddressAlias: String = "Me",
        encrypted: Boolean = false,
        packetId: String? = null
    )

    /**
     * Routes a text/data payload to a target node.
     *
     * @param targetId The destination mesh ID.
     * @param payload The payload data to send.
     * @param myAddressAlias The local node's alias.
     * @param encrypted Whether the payload should be encrypted.
     * @param packetId Optional custom packet ID.
     * @return [DispatchResult] indicating queue admission state.
     */
    suspend fun routePayload(
        targetId: String,
        payload: String,
        myAddressAlias: String = "Me",
        encrypted: Boolean = false,
        packetId: String? = null
    ): DispatchResult

    @Deprecated("Use routeMediaPacket instead", ReplaceWith("routeMediaPacket(packet)"))
    fun sendMediaPacket(packet: MeshPacket)

    /**
     * Routes a pre-constructed media packet.
     *
     * @param packet The media packet to route.
     * @return [DispatchResult] indicating queue admission state.
     */
    suspend fun routeMediaPacket(packet: MeshPacket): DispatchResult
}
