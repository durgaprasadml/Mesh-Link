package com.meshlink.ble.data.handlers

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.routing.engine.MeshTopologyManager
import com.meshlink.routing.engine.RouteManager
import com.meshlink.routing.engine.RoutingTable
import com.meshlink.util.MeshIdNormalizer
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeaconHandler @Inject constructor(
    private val topologyManager: MeshTopologyManager,
    private val routeManager: RouteManager,
    private val routingTable: RoutingTable
) {
    companion object {
        private const val TAG = "BeaconHandler"
    }

    /**
     * Generates a topology advertisement BEACON packet containing the local mesh ID
     * and a list of reachable multi-hop nodes and direct neighbors.
     */
    fun generateBeaconPacket(localMeshId: String): MeshPacket {
        val canonicalLocalId = MeshIdNormalizer.canonicalize(localMeshId)

        val jsonArray = JSONArray()
        // Include direct neighbors and known active routes
        val routes = routingTable.getAllRoutes().values.mapNotNull { it.firstOrNull() }
        routes.forEach { route ->
            if (route.destinationId != canonicalLocalId) {
                jsonArray.put(JSONObject().apply {
                    put("nodeId", MeshIdNormalizer.canonicalize(route.destinationId))
                    put("hops", route.hops)
                    put("transport", route.routeType.name)
                })
            }
        }

        val payloadObj = JSONObject().apply {
            put("nodeId", canonicalLocalId)
            put("reachable", jsonArray)
            put("timestamp", System.currentTimeMillis())
        }

        return MeshPacket(
            senderId = canonicalLocalId,
            targetId = "BROADCAST",
            payload = payloadObj.toString(),
            type = PacketType.BEACON,
            encrypted = false,
            ttl = 1,
            hopCount = 0
        )
    }

    /**
     * Handles an incoming BEACON topology advertisement packet.
     */
    fun handleBeaconPacket(packet: MeshPacket) {
        try {
            val senderId = MeshIdNormalizer.canonicalize(packet.senderId)
            if (senderId.isBlank()) return

            // 1. Update direct neighbor link info
            topologyManager.updateNeighbor(senderId, rssi = -65, transport = packet.transport)
            routeManager.updateRoute(
                destinationId = senderId,
                nextHop = senderId,
                hops = 1,
                rssi = -65,
                type = packet.transport
            )

            // 2. Parse advertised reachable topology
            val json = JSONObject(packet.payload)
            val reachableArray = json.optJSONArray("reachable") ?: JSONArray()
            val advertisedNodeIds = mutableListOf<String>()

            for (i in 0 until reachableArray.length()) {
                val nodeObj = reachableArray.optJSONObject(i) ?: continue
                val rawDestId = nodeObj.optString("nodeId", "")
                val destId = MeshIdNormalizer.canonicalize(rawDestId)
                val advertisedHops = nodeObj.optInt("hops", 1)
                val transportStr = nodeObj.optString("transport", RouteType.BLE.name)
                val transport = try { RouteType.valueOf(transportStr) } catch (_: Exception) { RouteType.BLE }

                if (destId.isNotBlank() && destId != senderId) {
                    advertisedNodeIds.add(destId)
                    // Add indirect route via senderId
                    routeManager.updateRoute(
                        destinationId = destId,
                        nextHop = senderId,
                        hops = advertisedHops + 1,
                        rssi = -70,
                        type = transport
                    )
                }
            }

            // 3. Update topology graph & recompute
            topologyManager.updateTopology(senderId, advertisedNodeIds)
            topologyManager.recomputeTopology()
            MeshLogger.d(TAG, "Processed BEACON from $senderId with ${advertisedNodeIds.size} advertised nodes")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling BEACON packet: ${e.message}")
        }
    }
}
