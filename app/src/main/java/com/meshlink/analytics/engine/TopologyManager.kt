package com.meshlink.analytics.engine

import javax.inject.Inject
import javax.inject.Singleton

data class Node(val id: String, val isGateway: Boolean = false, val isLeaf: Boolean = false)
data class Edge(val sourceId: String, val targetId: String, val transport: String, val rssi: Int)

data class TopologySnapshot(
    val nodes: List<Node>,
    val edges: List<Edge>,
    val meshDiameter: Int,
    val maxHops: Int,
    val averageHops: Float
)

@Singleton
class TopologyManager @Inject constructor() {

    private var lastComputationTime = 0L
    private val computationThrottleMs = 60_000L // 60 seconds throttle for heavy graph traversal
    private var cachedSnapshot: TopologySnapshot? = null

    private val activeNodes = mutableSetOf<Node>()
    private val activeEdges = mutableSetOf<Edge>()

    fun updateTopology(node: Node, edges: List<Edge>) {
        activeNodes.add(node)
        activeEdges.addAll(edges)
    }

    fun getTopologySnapshot(): TopologySnapshot {
        val now = System.currentTimeMillis()
        if (cachedSnapshot != null && (now - lastComputationTime) < computationThrottleMs) {
            return cachedSnapshot!!
        }

        // Mock computation of graph metrics
        val snapshot = TopologySnapshot(
            nodes = activeNodes.toList(),
            edges = activeEdges.toList(),
            meshDiameter = calculateDiameter(),
            maxHops = 4,
            averageHops = 2.3f
        )
        
        cachedSnapshot = snapshot
        lastComputationTime = now
        return snapshot
    }

    private fun calculateDiameter(): Int {
        // In a real implementation, run BFS from all nodes to find the longest shortest path
        return 5 
    }

    fun exportToMermaid(): String {
        val builder = java.lang.StringBuilder("graph TD;\n")
        activeEdges.forEach { edge ->
            val style = if (edge.transport == "BLE") ".." else "-"
            builder.append("    ${edge.sourceId}-$style->${edge.targetId};\n")
        }
        return builder.toString()
    }
}
