package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Route Repair Manager.
 * Detects broken links (relay disappearance or transmission failure),
 * attempts backup route failover, triggers local rediscovery, and propagates
 * Route Error (RERR) packets to notify upstream nodes.
 */
@Singleton
class RouteRepairManager @Inject constructor(
    private val routeCache: RouteCache,
    private val routeOptimizer: RouteOptimizer,
    private val discoveryEngine: RouteDiscoveryEngine,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "RouteRepairManager"
    }

    private val _repairCount = MutableStateFlow(0)
    val repairCount: StateFlow<Int> = _repairCount.asStateFlow()

    /**
     * Called when a peer/relay disconnects or a link breaks.
     * @param brokenNextHop The MAC/address of the link that went down.
     * @param localMeshId The local node's mesh ID.
     * @param sendPacketAction Function to broadcast packets (e.g. RERR).
     */
    fun handleLinkFailure(
        brokenNextHop: String,
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket, String?) -> Unit
    ) {
        applicationScope.launch {
            // Find all destinations using brokenNextHop
            val affectedDestinations = routeCache.getAllDestinations().filter { dest ->
                routeCache.getRoutesForDestination(dest).any { it.nextHop == brokenNextHop }
            }

            if (affectedDestinations.isEmpty()) return@launch

            MeshLogger.w(TAG, "Link break detected for $brokenNextHop! Affected destinations: ${affectedDestinations.size}")
            _repairCount.value += 1

            // 1. Remove broken route entries
            routeCache.removeRoutesViaHop(brokenNextHop)

            val unreachableDestinations = mutableListOf<String>()

            for (dest in affectedDestinations) {
                // Attempt 1: Alternative cached route
                val backupRoute = routeOptimizer.getBackupRoutes(dest, brokenNextHop).firstOrNull()
                if (backupRoute != null) {
                    MeshLogger.d(TAG) { "Local route repair succeeded for $dest: using backup route via ${backupRoute.nextHop}" }
                    continue
                }

                // Attempt 2: Local rediscovery
                MeshLogger.d(TAG) { "No backup route for $dest. Triggering target discovery..." }
                discoveryEngine.queueAndDiscover(
                    targetId = dest,
                    packet = null,
                    localMeshId = localMeshId,
                    sendPacketAction = { sendPacketAction(it, null) }
                )

                unreachableDestinations.add(dest)
            }

            // Attempt 3: Route Error (RERR) broadcast to notify upstream nodes
            if (unreachableDestinations.isNotEmpty()) {
                val rerrPayload = unreachableDestinations.joinToString(",")
                val rerrPacket = MeshPacket(
                    packetId = UUID.randomUUID().toString(),
                    senderId = localMeshId,
                    targetId = "BROADCAST",
                    payload = rerrPayload,
                    type = PacketType.ROUTE_ERROR,
                    priority = PacketPriority.HIGH,
                    ttl = 5,
                    hopCount = 0,
                    visitedPath = listOf(localMeshId)
                )

                MeshLogger.d(TAG) { "Broadcasting RERR for unreachable destinations: $rerrPayload" }
                sendPacketAction(rerrPacket, null)
            }
        }
    }

    /**
     * Handles an incoming RERR packet from upstream/downstream nodes.
     * Invalidates any routes leading to the unreachable destinations in payload.
     */
    fun handleRouteError(
        immediateSender: String,
        packet: MeshPacket,
        localMeshId: String
    ) {
        val unreachableDestinations = packet.payload.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (unreachableDestinations.isEmpty()) return

        MeshLogger.w(TAG, "Received RERR from $immediateSender for destinations: $unreachableDestinations")

        for (dest in unreachableDestinations) {
            val routes = routeCache.getRoutesForDestination(dest)
            val matchingRoutes = routes.filter { it.nextHop == immediateSender }
            if (matchingRoutes.isNotEmpty()) {
                routeCache.removeRoutesViaHop(immediateSender)
                MeshLogger.d(TAG) { "Invalidated route to $dest via broken hop $immediateSender due to RERR" }
            }
        }
    }
}
