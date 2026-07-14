package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkTopologyEngine @Inject constructor(
    private val routeCache: RouteCache
) {
    companion object {
        private const val TAG = "NetworkTopologyEngine"
    }

    // Graph of node ID to its neighbors
    private val networkGraph = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Tracks partitions. If a new node joins and tells us about nodes we thought were missing,
    // we might have merged with another partition.
    private val knownIslands = ConcurrentHashMap<String, Long>()

    fun updateTopology(nodeId: String, neighbors: List<String>) {
        networkGraph.putIfAbsent(nodeId, ConcurrentHashMap.newKeySet())
        val currentNeighbors = networkGraph[nodeId]!!
        
        val added = neighbors.filter { it !in currentNeighbors }
        val removed = currentNeighbors.filter { it !in neighbors }
        
        if (added.isNotEmpty() || removed.isNotEmpty()) {
            currentNeighbors.clear()
            currentNeighbors.addAll(neighbors)
            
            // If a large number of nodes suddenly became reachable, it's a partition merge
            if (added.size > 5) {
                handlePartitionMerge(added)
            }
        }
    }

    fun removeNode(nodeId: String) {
        networkGraph.remove(nodeId)
        networkGraph.values.forEach { it.remove(nodeId) }
    }
    
    fun getNetworkDiameter(): Int {
        // Simplified diameter estimation based on max known hops
        var maxHops = 0
        routeCache.getAllDestinations().forEach { dest ->
            routeCache.getRoutesForDestination(dest).forEach { route ->
                if (route.hops > maxHops) maxHops = route.hops
            }
        }
        return maxHops
    }
    
    fun getReachabilityGraph(): Map<String, Set<String>> {
        return networkGraph.toMap()
    }

    private fun handlePartitionMerge(newNodes: List<String>) {
        MeshLogger.d(TAG, "Partition Merge detected! ${newNodes.size} nodes joined.")
        // In a real scenario, we would trigger a high-priority sync of SOS and critical metadata.
        // We limit it to high priority only to prevent broadcast storms.
    }
}
