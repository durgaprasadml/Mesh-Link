package com.meshlink.routing.engine

import com.meshlink.domain.model.RouteType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReachableNode(
    val nodeId: String,
    val viaRelayId: String,
    val hopCount: Int,
    val rssi: Int = -70,
    val transport: RouteType = RouteType.BLE,
    val routeStability: Float = 1.0f,
    val lastSeen: Long = System.currentTimeMillis()
)

data class NeighborInfo(
    val nodeId: String,
    val rssi: Int = -65,
    val transport: RouteType = RouteType.BLE,
    val lastSeen: Long = System.currentTimeMillis()
)

data class TopologyMetrics(
    val totalNodes: Int = 0,
    val directNeighbors: Int = 0,
    val meshDiameter: Int = 0,
    val averageHopCount: Float = 0.0f,
    val recoveryCount: Int = 0,
    val forwardedCount: Long = 0L,
    val droppedCount: Long = 0L,
    val duplicateCount: Long = 0L,
    val networkHealth: Float = 1.0f
)

/**
 * Manages network topology graph, reachability computations, and network health metrics.
 */
@Singleton
class MeshTopologyManager @Inject constructor(
    private val routingTable: RoutingTable
) {
    // Node ID -> set of its neighbors
    private val networkGraph = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Direct physical neighbors
    private val directNeighbors = ConcurrentHashMap<String, NeighborInfo>()
    
    // Reachable multi-hop mesh nodes
    private val _reachableNodes = MutableStateFlow<List<ReachableNode>>(emptyList())
    val reachableNodes: StateFlow<List<ReachableNode>> = _reachableNodes.asStateFlow()

    // Real-time topology graph
    private val _topologyGraph = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val topologyGraph: StateFlow<Map<String, Set<String>>> = _topologyGraph.asStateFlow()

    // Network metrics
    private val _metrics = MutableStateFlow(TopologyMetrics())
    val metrics: StateFlow<TopologyMetrics> = _metrics.asStateFlow()

    /**
     * Updates direct neighbor link information.
     */
    fun updateNeighbor(nodeId: String, rssi: Int, transport: RouteType = RouteType.BLE) {
        directNeighbors[nodeId] = NeighborInfo(nodeId = nodeId, rssi = rssi, transport = transport)
        recomputeTopology()
    }

    /**
     * Removes a direct neighbor when disconnected.
     */
    fun removeNeighbor(nodeId: String) {
        directNeighbors.remove(nodeId)
        networkGraph.remove(nodeId)
        networkGraph.values.forEach { it.remove(nodeId) }
        routingTable.invalidateRoutesViaHop(nodeId)
        recordRecoveryEvent()
        recomputeTopology()
    }

    /**
     * Updates adjacency graph from topology advertisements.
     */
    fun updateTopology(nodeId: String, neighborIds: List<String>) {
        networkGraph.putIfAbsent(nodeId, ConcurrentHashMap.newKeySet())
        val neighbors = networkGraph[nodeId]!!
        neighbors.clear()
        neighbors.addAll(neighborIds)
        recomputeTopology()
    }

    fun recordForward() {
        _metrics.value = _metrics.value.copy(forwardedCount = _metrics.value.forwardedCount + 1)
    }

    fun recordDrop() {
        _metrics.value = _metrics.value.copy(droppedCount = _metrics.value.droppedCount + 1)
    }

    fun recordDuplicate() {
        _metrics.value = _metrics.value.copy(duplicateCount = _metrics.value.duplicateCount + 1)
    }

    fun recordRecoveryEvent() {
        _metrics.value = _metrics.value.copy(recoveryCount = _metrics.value.recoveryCount + 1)
    }

    /**
     * Recomputes network reachability, average hop count, mesh diameter, and health.
     */
    fun recomputeTopology() {
        _topologyGraph.value = networkGraph.mapValues { it.value.toSet() }

        val activeRoutes = routingTable.getAllRoutes()
        val reachable = mutableListOf<ReachableNode>()

        activeRoutes.forEach { (dest, routes) ->
            val best = routes.firstOrNull() ?: return@forEach
            if (best.nextHop != dest) { // Multi-hop node
                reachable.add(
                    ReachableNode(
                        nodeId = dest,
                        viaRelayId = best.nextHop,
                        hopCount = best.hops,
                        rssi = best.metrics.rssi,
                        transport = best.routeType,
                        routeStability = best.metrics.routeStability,
                        lastSeen = best.lastSeen
                    )
                )
            }
        }

        _reachableNodes.value = reachable

        val allHops = activeRoutes.values.mapNotNull { it.firstOrNull()?.hops }
        val avgHops = if (allHops.isNotEmpty()) allHops.average().toFloat() else 0f
        val maxHops = if (allHops.isNotEmpty()) allHops.maxOrNull() ?: 0 else 0

        _metrics.value = _metrics.value.copy(
            totalNodes = networkGraph.size,
            directNeighbors = directNeighbors.size,
            meshDiameter = maxHops,
            averageHopCount = avgHops,
            networkHealth = computeHealthScore(avgHops)
        )
    }

    private fun computeHealthScore(avgHops: Float): Float {
        val dropRate = if (_metrics.value.forwardedCount > 0) {
            _metrics.value.droppedCount.toFloat() / (_metrics.value.forwardedCount + _metrics.value.droppedCount)
        } else 0f
        return (1.0f - dropRate).coerceIn(0.0f, 1.0f)
    }
}
