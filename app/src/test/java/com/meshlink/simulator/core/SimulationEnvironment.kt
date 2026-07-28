package com.meshlink.simulator.core

import com.meshlink.simulator.metrics.NetworkRecorder
import com.meshlink.simulator.metrics.SimulationMetrics
import com.meshlink.simulator.node.SimulatedNode
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyExporter
import com.meshlink.simulator.transport.Link
import com.meshlink.simulator.transport.SimulatedTransport
import com.meshlink.simulator.transport.TransportConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Top-level orchestrator for a mesh simulation run.
 *
 * [SimulationEnvironment] owns:
 * - All [SimulatedNode]s in the simulation
 * - The shared [SimulatedClock]
 * - The shared [SimulationScheduler]
 * - The shared [NetworkRecorder]
 * - The shared [SimulationMetrics]
 * - All [Link]s (bidirectional virtual connections)
 *
 * The environment wires nodes together by registering delivery callbacks
 * between paired [SimulatedTransport] instances.
 *
 * Topology changes (add/remove links, partitions, heals) can happen at any
 * time and are immediately reflected in subsequent packet routing.
 *
 * Usage:
 * ```kotlin
 * val env = SimulationEnvironment.build {
 *     nodes(listOf("A","B","C"))
 *     topology(TopologyBuilder.ring(listOf("A","B","C")))
 *     profile(NetworkProfile.PerfectNetwork)
 * }
 * env.node("A").sendPacket("C", "hello")
 * env.step(1_000)
 * env.runUntilQuiet()
 * assertTrue(env.node("C").receivedPackets().isNotEmpty())
 * ```
 */
class SimulationEnvironment private constructor(
    val clock: SimulatedClock,
    val scheduler: SimulationScheduler,
    val recorder: NetworkRecorder,
    val metrics: SimulationMetrics,
    private val _nodes: MutableMap<String, SimulatedNode>,
    private val _links: MutableList<Link>
) {
    val exporter = TopologyExporter()

    // ── Node access ───────────────────────────────────────────────────────────────

    /** All nodes in this simulation. */
    val nodes: Collection<SimulatedNode> get() = _nodes.values

    /** Returns the node with [meshId], or throws [IllegalArgumentException]. */
    fun node(meshId: String): SimulatedNode =
        _nodes[meshId] ?: throw IllegalArgumentException("Node '$meshId' not found. Available: ${_nodes.keys}")

    /** Returns the node at [index] in insertion order. */
    fun nodeAt(index: Int): SimulatedNode = _nodes.values.toList()[index]

    /** All node IDs. */
    fun nodeIds(): List<String> = _nodes.keys.toList()

    // ── Simulation execution ──────────────────────────────────────────────────────

    /**
     * Advances the virtual clock by [virtualMs] and processes all events
     * scheduled within that window, then drains all node queues.
     */
    fun step(virtualMs: Long = 100) {
        scheduler.runFor(virtualMs)
        _nodes.values.filter { it.isOnline }.forEach { it.drainQueue() }
    }

    /**
     * Runs the simulation until no packets are in-flight (all queues empty and
     * no pending scheduled events), or until [maxStepMs] of virtual time has elapsed.
     *
     * @param stepMs  Virtual ms advanced per iteration.
     * @param maxStepMs Maximum total virtual ms before giving up.
     * @return The number of steps taken.
     */
    fun runUntilQuiet(stepMs: Long = 50, maxStepMs: Long = 30_000): Int {
        var steps = 0
        var elapsed = 0L
        while (elapsed < maxStepMs) {
            val hadPending = scheduler.pendingCount() > 0 ||
                             _nodes.values.any { it.isOnline && it.queueOptimizer.size() > 0 }
            step(stepMs)
            steps++
            elapsed += stepMs
            if (!hadPending) break
        }
        metrics.simulationEndMs = clock.currentTimeMs
        return steps
    }

    // ── Topology management ───────────────────────────────────────────────────────

    /**
     * Adds a bidirectional link between [nodeA] and [nodeB] with [config].
     * Also updates [SimulationMetrics.routeConvergenceTimeMs] start time.
     */
    fun addLink(nodeIdA: String, nodeIdB: String, config: TransportConfig = TransportConfig.TypicalBle) {
        val linkAB = Link(nodeIdA, nodeIdB, config)
        val linkBA = Link(nodeIdB, nodeIdA, config)
        wireLink(linkAB)
        wireLink(linkBA)
        _links.addAll(listOf(linkAB, linkBA))
    }

    /**
     * Removes all links between [nodeIdA] and [nodeIdB] (both directions).
     */
    fun removeLink(nodeIdA: String, nodeIdB: String) {
        _links.removeIf {
            (it.fromNodeId == nodeIdA && it.toNodeId == nodeIdB) ||
            (it.fromNodeId == nodeIdB && it.toNodeId == nodeIdA)
        }
        _nodes[nodeIdA]?.transport?.removeLink(nodeIdB)
        _nodes[nodeIdB]?.transport?.removeLink(nodeIdA)
    }

    /**
     * Severs all links between [groupA] and [groupB] to simulate a network partition.
     * Links within each group are preserved.
     */
    fun partition(groupA: List<String>, groupB: List<String>) {
        groupA.forEach { a -> groupB.forEach { b -> removeLink(a, b) } }
    }

    /**
     * Restores bidirectional links between [groupA] and [groupB] after a partition.
     * Uses [config] for the restored links.
     */
    fun heal(groupA: List<String>, groupB: List<String>, config: TransportConfig = TransportConfig.TypicalBle) {
        groupA.forEach { a -> groupB.forEach { b -> addLink(a, b, config) } }
        groupA.forEach { a -> _nodes[a]?.comeOnline() }
        groupB.forEach { b -> _nodes[b]?.comeOnline() }
        metrics.routeConvergenceTimeMs.set(clock.currentTimeMs)
    }

    /**
     * Applies a [NetworkProfile] to all links in the simulation.
     */
    fun applyProfile(profile: NetworkProfile) {
        _links.forEach { it.applyConfig(profile.config) }
    }

    /**
     * Applies a [NetworkProfile] to the link between [fromNodeId] and [toNodeId] only.
     */
    fun applyProfileToLink(fromNodeId: String, toNodeId: String, profile: NetworkProfile) {
        _links.find { it.fromNodeId == fromNodeId && it.toNodeId == toNodeId }
            ?.applyConfig(profile.config)
    }

    // ── Failure injection ─────────────────────────────────────────────────────────

    /** Takes a node offline — packets addressed to it are stored for later. */
    fun goOffline(nodeId: String) {
        node(nodeId).goOffline()
        _links.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }.forEach { it.disable() }
    }

    /** Brings a node back online and triggers store-and-forward delivery. */
    fun comeOnline(nodeId: String) {
        node(nodeId).comeOnline()
        _links.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }.forEach { it.enable() }
        // Trigger S&F on peers that might have queued packets for this node
        _nodes.values.forEach { if (it.isOnline) it.tryDeliverStored() }
    }

    /** Simulates a node crash — clears all in-memory relay state. */
    fun crash(nodeId: String) {
        node(nodeId).crash()
        _links.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }.forEach { it.disable() }
    }

    /** Restarts a crashed node with fresh state. */
    fun restart(nodeId: String) {
        node(nodeId).restart()
        _links.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }.forEach { it.enable() }
    }

    /** Disables a specific directed link (packet loss = 100% on that link). */
    fun disableLink(fromNodeId: String, toNodeId: String) {
        _links.find { it.fromNodeId == fromNodeId && it.toNodeId == toNodeId }?.disable()
    }

    /** Re-enables a previously disabled link. */
    fun enableLink(fromNodeId: String, toNodeId: String) {
        _links.find { it.fromNodeId == fromNodeId && it.toNodeId == toNodeId }?.enable()
    }

    // ── Export ────────────────────────────────────────────────────────────────────

    fun exportDot(): String = exporter.exportDot(_nodes.keys.toList(), _links)
    fun exportMermaid(): String = exporter.exportMermaid(_nodes.keys.toList(), _links)
    fun exportJson(): String = exporter.exportJson(_nodes.keys.toList(), _links)

    // ── Introspection ─────────────────────────────────────────────────────────────

    /** Snapshot of all current links. */
    fun allLinks(): List<Link> = _links.toList()

    /** Number of nodes in the simulation. */
    val nodeCount: Int get() = _nodes.size

    override fun toString(): String =
        "SimulationEnvironment(nodes=${_nodes.size}, links=${_links.size}, t=${clock.currentTimeMs}ms)"

    // ── Internal wiring ───────────────────────────────────────────────────────────

    private fun wireLink(link: Link) {
        val fromNode = _nodes[link.fromNodeId] ?: return
        val toNode = _nodes[link.toNodeId] ?: return
        fromNode.transport.addLink(link)
        // Register delivery callback: when fromNode sends to toNode, toNode's handler is called
        fromNode.transport.registerNodeDelivery(link.toNodeId) { sender, packet ->
            toNode.onPacketReceived(sender, packet)
        }
        // Also register the reverse so the transport knows the peer is reachable
        toNode.transport.registerNodeDelivery(link.fromNodeId) { sender, packet ->
            fromNode.onPacketReceived(sender, packet)
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────────

    companion object {
        /**
         * Creates a [SimulationEnvironment] from the given nodes and links.
         *
         * @param nodes    Pre-constructed [SimulatedNode] instances.
         * @param links    Directed links to wire between nodes.
         * @param seed     Random seed for transport decisions.
         */
        fun create(
            nodes: List<SimulatedNode>,
            links: List<Link>,
            clock: SimulatedClock = SimulatedClock(),
            scheduler: SimulationScheduler = SimulationScheduler(clock),
            recorder: NetworkRecorder = NetworkRecorder()
        ): SimulationEnvironment {
            val metrics = SimulationMetrics().also { m ->
                nodes.forEach { n -> m.registerNode(n.metrics) }
                m.simulationStartMs = clock.currentTimeMs
            }

            val nodeMap = nodes.associateBy { it.meshId }.toMutableMap()
            val linkList = links.toMutableList()

            val env = SimulationEnvironment(clock, scheduler, recorder, metrics, nodeMap, linkList)
            links.forEach { env.wireLink(it) }
            return env
        }
    }
}
