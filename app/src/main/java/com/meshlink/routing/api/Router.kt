package com.meshlink.routing.api

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.RouteEntry
import kotlinx.coroutines.flow.SharedFlow

/**
 * Handles routing of packets across the mesh network.
 *
 * Responsibility: Determine the best path for packets and manage the routing table.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe for concurrent routing.
 * Return Contract: Routing operations return [MeshResult] to indicate success or routing failure.
 * Failure Conditions: Route not found, target unreachable, TTL expired.
 */
interface Router {
    var localMeshId: String
    val incomingPayloads: SharedFlow<Pair<String, MeshPacket>>
    val routeTable: Map<String, RouteEntry>

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
     * @return [MeshResult.Success] if routed successfully, [MeshResult.Error] if routing fails.
     */
    suspend fun routePayload(
        targetId: String,
        payload: String,
        myAddressAlias: String = "Me",
        encrypted: Boolean = false,
        packetId: String? = null
    ): MeshResult<Unit>

    @Deprecated("Use routeMediaPacket instead", ReplaceWith("routeMediaPacket(packet)"))
    fun sendMediaPacket(packet: MeshPacket)

    /**
     * Routes a pre-constructed media packet.
     *
     * @param packet The media packet to route.
     * @return [MeshResult.Success] if routed successfully, [MeshResult.Error] if routing fails.
     */
    suspend fun routeMediaPacket(packet: MeshPacket): MeshResult<Unit>
}
