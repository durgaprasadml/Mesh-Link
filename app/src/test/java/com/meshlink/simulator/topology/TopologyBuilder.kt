package com.meshlink.simulator.topology

import com.meshlink.simulator.transport.Link
import com.meshlink.simulator.transport.TransportConfig

/**
 * Factory object that creates standard mesh network topologies as lists of [Link]s.
 *
 * Each factory returns a list of directed [Link] pairs (bidirectional by default).
 * Pass the result to [SimulationEnvironment] to wire the nodes together.
 *
 * Topologies available:
 * - [line] — linear chain A–B–C–…
 * - [star] — hub with n–1 spokes
 * - [ring] — circular; each node connects to 2 neighbors
 * - [tree] — n-ary tree
 * - [fullyConnected] — every node connected to every other
 * - [randomMesh] — Erdős–Rényi random graph with configurable density
 *
 * Dynamic topology changes are supported via [SimulationEnvironment.addLink] /
 * [SimulationEnvironment.removeLink] during a running simulation.
 */
object TopologyBuilder {

    /**
     * Creates a linear chain: node[0] ↔ node[1] ↔ … ↔ node[n-1].
     * Each adjacent pair has one link in each direction.
     *
     * @param nodeIds Ordered list of node IDs.
     * @param config  Transport config applied to all links.
     */
    fun line(nodeIds: List<String>, config: TransportConfig = TransportConfig.TypicalBle): List<Link> {
        require(nodeIds.size >= 2) { "line topology requires at least 2 nodes" }
        return buildBidirectional(nodeIds.zipWithNext(), config)
    }

    /**
     * Creates a star topology: [hubId] ↔ every other node.
     *
     * @param hubId   The central hub node ID.
     * @param spokeIds Peripheral node IDs.
     * @param config  Transport config for all hub-spoke links.
     */
    fun star(
        hubId: String,
        spokeIds: List<String>,
        config: TransportConfig = TransportConfig.TypicalBle
    ): List<Link> {
        require(spokeIds.isNotEmpty()) { "star topology requires at least 1 spoke" }
        return buildBidirectional(spokeIds.map { hubId to it }, config)
    }

    /**
     * Creates a ring topology: each node connects to its two neighbors.
     * node[0] ↔ node[1] ↔ … ↔ node[n-1] ↔ node[0]
     *
     * @param nodeIds Ordered list of node IDs (minimum 3).
     * @param config  Transport config for all links.
     */
    fun ring(nodeIds: List<String>, config: TransportConfig = TransportConfig.TypicalBle): List<Link> {
        require(nodeIds.size >= 3) { "ring topology requires at least 3 nodes" }
        val pairs = nodeIds.zipWithNext() + (nodeIds.last() to nodeIds.first())
        return buildBidirectional(pairs, config)
    }

    /**
     * Creates an n-ary tree topology.
     *
     * @param nodeIds    Ordered list of node IDs (BFS order: root first, then children level by level).
     * @param branching  Number of children per node (default 2 = binary tree).
     * @param config     Transport config for all links.
     */
    fun tree(
        nodeIds: List<String>,
        branching: Int = 2,
        config: TransportConfig = TransportConfig.TypicalBle
    ): List<Link> {
        require(nodeIds.isNotEmpty()) { "tree topology requires at least 1 node" }
        require(branching >= 1) { "branching must be >= 1" }
        val pairs = mutableListOf<Pair<String, String>>()
        for (i in nodeIds.indices) {
            for (b in 1..branching) {
                val childIndex = i * branching + b
                if (childIndex < nodeIds.size) {
                    pairs.add(nodeIds[i] to nodeIds[childIndex])
                }
            }
        }
        return buildBidirectional(pairs, config)
    }

    /**
     * Creates a fully connected (complete graph) topology.
     * Every node has a direct link to every other node.
     *
     * ⚠️ Use with care for large n — link count is O(n²).
     *
     * @param nodeIds List of node IDs.
     * @param config  Transport config for all links.
     */
    fun fullyConnected(
        nodeIds: List<String>,
        config: TransportConfig = TransportConfig.TypicalBle
    ): List<Link> {
        require(nodeIds.size >= 2) { "fullyConnected topology requires at least 2 nodes" }
        val pairs = mutableListOf<Pair<String, String>>()
        for (i in nodeIds.indices) {
            for (j in i + 1 until nodeIds.size) {
                pairs.add(nodeIds[i] to nodeIds[j])
            }
        }
        return buildBidirectional(pairs, config)
    }

    /**
     * Creates a random mesh (Erdős–Rényi G(n, p) model).
     *
     * Each pair of nodes is connected with probability [density] (0.0..1.0).
     * A minimum spanning tree is added afterwards to guarantee connectivity.
     *
     * @param nodeIds Ordered list of node IDs.
     * @param density Edge probability (0.0 = no edges, 1.0 = fully connected).
     * @param seed    Random seed for reproducibility.
     * @param config  Transport config for all links.
     */
    fun randomMesh(
        nodeIds: List<String>,
        density: Float = 0.3f,
        seed: Long = 42L,
        config: TransportConfig = TransportConfig.TypicalBle
    ): List<Link> {
        require(nodeIds.size >= 2) { "randomMesh topology requires at least 2 nodes" }
        require(density in 0f..1f) { "density must be in [0, 1]" }

        val random = java.util.Random(seed)
        val pairs = mutableSetOf<Pair<String, String>>()

        // Erdős–Rényi random edges
        for (i in nodeIds.indices) {
            for (j in i + 1 until nodeIds.size) {
                if (random.nextFloat() < density) {
                    pairs.add(nodeIds[i] to nodeIds[j])
                }
            }
        }

        // Guarantee connectivity via a simple spanning path
        for (i in 0 until nodeIds.size - 1) {
            pairs.add(nodeIds[i] to nodeIds[i + 1])
        }

        return buildBidirectional(pairs.toList(), config)
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    /**
     * Creates bidirectional [Link] pairs from a list of (from, to) pairs.
     * Each pair generates two directed links.
     */
    private fun buildBidirectional(
        pairs: List<Pair<String, String>>,
        config: TransportConfig
    ): List<Link> = pairs.flatMap { (a, b) ->
        listOf(
            Link(fromNodeId = a, toNodeId = b, config = config),
            Link(fromNodeId = b, toNodeId = a, config = config)
        )
    }
}
