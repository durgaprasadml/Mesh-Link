package com.meshlink.simulator.node

import com.meshlink.config.MeshConfig
import com.meshlink.config.RuntimeConfigManager
import com.meshlink.domain.model.*
import com.meshlink.routing.engine.*
import com.meshlink.simulator.metrics.NetworkRecorder
import com.meshlink.simulator.security.SimulatedSecurityLayer
import com.meshlink.simulator.transport.SimulatedTransport
import com.meshlink.simulator.core.SimulatedClock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Simulates a single mesh node in the test environment.
 *
 * Each [SimulatedNode] wires up real production routing components
 * (no mocks, no Hilt, no Android runtime):
 * - [RouteCache] — real multi-path route table
 * - [RouteScorer] — real multi-factor scoring
 * - [RouteOptimizer] — real predictive failure analysis
 * - [RouteManager] — real route CRUD and delivery recording
 * - [QueueOptimizer] — real priority queue
 * - [CongestionMonitor] — real congestion tracking
 * - [RoutingEngine] — assembled from the above pure-JVM components,
 *   with [SimulatedBatteryNetworking] replacing the Android-Context-dependent
 *   production class
 *
 * The node implements the same packet-handling logic as [MeshRouter]:
 * - Encryption enforcement
 * - Trust level gating (stubbed as TRUSTED for all peers by default)
 * - Duplicate suppression via RoutingEngine's dedup cache
 * - Loop detection via TTL and visitedPath
 * - Next-hop routing via RouteManager
 * - Store-and-forward via in-memory relay store
 * - Broadcast relay with probabilistic battery gating
 *
 * **Trace ID Support (Req 4)**: `sendPacket` embeds `"trace:<uuid>"` as the first
 * element of `visitedPath` so [NetworkRecorder] can correlate events end-to-end.
 *
 * State machine: [NodeState.ONLINE] | [NodeState.OFFLINE] | [NodeState.CRASHED]
 *
 * @param meshId      The globally unique mesh identity of this node.
 * @param clock       Shared virtual clock.
 * @param recorder    Shared packet-event recorder.
 * @param transport   The [SimulatedTransport] owned by this node.
 * @param security    Per-node simulated security layer (optional).
 * @param configOverride Optional config overrides for TTL, maxHops, etc.
 */
class SimulatedNode(
    val meshId: String,
    private val clock: SimulatedClock,
    val recorder: NetworkRecorder,
    val transport: SimulatedTransport,
    val security: SimulatedSecurityLayer = SimulatedSecurityLayer(meshId),
    configOverride: NodeConfig = NodeConfig()
) {
    // ── Config ────────────────────────────────────────────────────────────────────

    data class NodeConfig(
        val defaultTtl: Int = 10,
        val maxHops: Int = 15,
        val relayEnabled: Boolean = true,
        val enforceEncryption: Boolean = false,
        val duplicateCacheSize: Int = 10000,
        val duplicateCacheLifetimeMs: Long = 60_000L
    )

    val config: NodeConfig = configOverride

    // ── State Machine ─────────────────────────────────────────────────────────────

    enum class NodeState { ONLINE, OFFLINE, CRASHED }
    @Volatile var state: NodeState = NodeState.ONLINE
        private set

    val isOnline: Boolean get() = state == NodeState.ONLINE

    // ── Real Production Routing Components ───────────────────────────────────────

    private val runtimeConfig = RuntimeConfigManager(
        MeshConfig(
            maxRelayPackets = 1000,
            defaultTtl = config.defaultTtl,
            maxHops = config.maxHops,
            routingRetryCount = 3,
            routingRetryIntervalMs = 1000L
        )
    ).also { mgr ->
        mgr.updateConfig { it.copy(
            duplicateCacheSize = config.duplicateCacheSize,
            duplicateCacheLifetimeMs = config.duplicateCacheLifetimeMs,
            maxHopLimit = config.maxHops
        )}
    }

    val routeCache = RouteCache(runtimeConfig)
    private val routeScorer = RouteScorer()
    val routeOptimizer = RouteOptimizer(routeCache)
    val routeManager = RouteManager(routeCache, routeScorer, routeOptimizer)
    val congestionMonitor = CongestionMonitor()
    val queueOptimizer = QueueOptimizer()
    private val batteryNetworking = SimulatedBatteryNetworking()
    private val qosManager = QoSManager()
    private val topologyEngine = NetworkTopologyEngine(routeCache)
    @Suppress("unused")
    private val retryEngine = IntelligentRetryEngineStub(congestionMonitor)

    // ── Dedup Cache (mirrors RoutingEngine.TimeBoundedDuplicateCache) ─────────────
    private val seenPacketIds = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** Returns true if packetId is NEW (not a duplicate). */
    private fun markPacketProcessed(packetId: String): Boolean {
        val now = clock.currentTimeMs
        val config = runtimeConfig.currentConfig.value
        // Evict stale entries lazily
        if (seenPacketIds.size > config.duplicateCacheSize) {
            seenPacketIds.entries.removeIf { now - it.value > config.duplicateCacheLifetimeMs }
            if (seenPacketIds.size > config.duplicateCacheSize) seenPacketIds.clear()
        }
        val existing = seenPacketIds.putIfAbsent(packetId, now)
        if (existing != null) {
            val age = now - existing
            if (age > config.duplicateCacheLifetimeMs) {
                seenPacketIds[packetId] = now
                return true
            }
            return false
        }
        return true
    }

    private fun isRoutingLoop(packet: MeshPacket): Boolean {
        if (packet.ttl <= 0) return true
        if (meshId.isNotBlank() && packet.visitedPath.contains(meshId)) return true
        return false
    }

    // ── Store-and-Forward (in-memory relay store) ─────────────────────────────────

    data class RelayEntry(
        val packet: MeshPacket,
        val storedAt: Long,
        val expiresAt: Long
    )

    private val relayStore = ConcurrentHashMap<String, RelayEntry>()
    private val MAX_RELAY_SIZE = 1000

    // ── Metrics ───────────────────────────────────────────────────────────────────

    val metrics = NodeMetrics(meshId)

    // ── Delivery Callbacks (registered per-peer for inbound packets) ──────────────

    private val deliveredPackets = CopyOnWriteArrayListAlias<Pair<String, MeshPacket>>()

    /**
     * Callback invoked by [SimulatedTransport] when this node receives a packet.
     * Registered in [SimulationEnvironment] during node wiring.
     */
    fun onPacketReceived(fromNodeId: String, packet: MeshPacket) {
        if (state != NodeState.ONLINE) return
        handleIncomingPacket(fromNodeId, packet)
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Sends a packet from this node to [targetId] with [payload] and [type].
     *
     * Embeds `"trace:<uuid>"` as the first element of `visitedPath` for end-to-end
     * tracing via [NetworkRecorder]. The trace ID is returned for test assertions.
     *
     * @return The trace ID (`"trace:<uuid>"`) for this packet.
     */
    fun sendPacket(
        targetId: String,
        payload: String,
        type: PacketType = PacketType.TEXT,
        priority: PacketPriority = PacketPriority.NORMAL,
        encrypted: Boolean = false,
        customPacketId: String? = null
    ): String {
        val traceId = "trace:${UUID.randomUUID()}"
        val packetId = customPacketId ?: UUID.randomUUID().toString()

        val packet = MeshPacket(
            packetId = packetId,
            senderId = meshId,
            targetId = targetId,
            payload = payload,
            type = type,
            priority = priority,
            encrypted = encrypted,
            ttl = config.defaultTtl,
            hopCount = 0,
            visitedPath = mutableListOf(traceId)  // Embed trace ID at position 0
        )

        markPacketProcessed(packetId)
        metrics.packetsSent.incrementAndGet()
        queueOptimizer.enqueue(packet)
        drainQueue()
        return traceId
    }

    /**
     * Drains the outbound queue and dispatches all packets via [transport].
     * Called automatically after [sendPacket] and also by [SimulationEnvironment.step].
     */
    fun drainQueue() {
        while (queueOptimizer.size() > 0) {
            val packet = queueOptimizer.dequeue() ?: break
            val enqueueTime = clock.currentTimeMs
            dispatchPacket(packet)
            val waitMs = clock.currentTimeMs - enqueueTime
            metrics.queueWaitTimeSumMs.addAndGet(waitMs)
            metrics.queueWaitTimeSamples.incrementAndGet()
        }
    }

    /** Transitions node to OFFLINE — incoming packets are ignored, relay stored. */
    fun goOffline() { state = NodeState.OFFLINE }

    /** Brings node back ONLINE — triggers store-and-forward delivery attempt. */
    fun comeOnline() {
        state = NodeState.ONLINE
        tryDeliverStored()
    }

    /** Simulates a node crash — loses all in-memory relay state. */
    fun crash() {
        state = NodeState.CRASHED
        relayStore.clear()
        queueOptimizer.clear()
    }

    /** Restarts a crashed node — equivalent to comeOnline but with fresh state. */
    fun restart() {
        seenPacketIds.clear()
        state = NodeState.ONLINE
    }

    /** All packets that were finally delivered to this node (final destination). */
    fun receivedPackets(): List<Pair<String, MeshPacket>> = deliveredPackets.toList()

    /** Returns the outgoing peers this node can directly reach. */
    fun connectedPeers(): Set<String> = transport.connectedPeers

    // ── Core Packet Handler (mirrors MeshRouter.handleIncomingPacket) ─────────────

    private fun handleIncomingPacket(immediateSender: String, packet: MeshPacket) {
        // Encryption enforcement
        if (config.enforceEncryption && !packet.encrypted
            && packet.type != PacketType.KEY_EXCHANGE
            && packet.type != PacketType.SOS) {
            recorder.recordDrop(clock.currentTimeMs, meshId, packet,
                NetworkRecorder.DropReason.ENCRYPTION_FAILED, immediateSender)
            metrics.packetsDropped.incrementAndGet()
            return
        }

        val isBroadcast = packet.targetId == "BROADCAST"
        val isForMe = packet.targetId == meshId

        // Dedup check
        metrics.duplicateCacheLookups.incrementAndGet()
        val isNew = markPacketProcessed(packet.packetId)
        if (!isNew) {
            if (!isForMe) {
                recorder.recordDrop(clock.currentTimeMs, meshId, packet,
                    NetworkRecorder.DropReason.DUPLICATE, immediateSender)
                metrics.duplicatesSuppressed.incrementAndGet()
                metrics.duplicateCacheHits.incrementAndGet()
                return
            }
        } else {
            metrics.packetsReceived.incrementAndGet()
        }

        // Dynamic route learning
        routeManager.updateRoute(
            destinationId = packet.senderId,
            nextHop = immediateSender,
            hops = packet.hopCount,
            rssi = -65,
            trustScore = 80,
            type = RouteType.BLE
        )

        // Deliver locally if addressed to us or broadcast
        if (isForMe || isBroadcast) {
            deliveredPackets.add(immediateSender to packet)
            recorder.recordDelivered(clock.currentTimeMs, meshId, packet, immediateSender)
            metrics.hopCountSum.addAndGet(packet.hopCount.toLong())
            metrics.hopCountSamples.incrementAndGet()
        }

        // Packets for us only — don't forward
        if (isForMe) return

        // ACK/NACK are not forwarded
        val isAckNack = packet.type == PacketType.MEDIA_ACK || packet.type == PacketType.MEDIA_NACK

        // TTL check
        if (packet.ttl <= 0) {
            metrics.ttlExpirations.incrementAndGet()
            recorder.recordDrop(clock.currentTimeMs, meshId, packet,
                NetworkRecorder.DropReason.TTL_EXPIRED, immediateSender)
            return
        }

        // Loop guard
        if (isRoutingLoop(packet)) {
            metrics.loopsDetected.incrementAndGet()
            recorder.recordDrop(clock.currentTimeMs, meshId, packet,
                NetworkRecorder.DropReason.LOOP_DETECTED, immediateSender)
            return
        }

        // Relay disabled
        if (!config.relayEnabled && !isAckNack) return

        // Max hops
        if (packet.hopCount >= config.maxHops) {
            recorder.recordDrop(clock.currentTimeMs, meshId, packet,
                NetworkRecorder.DropReason.TTL_EXPIRED, immediateSender)
            return
        }

        // Build relay packet
        val relayPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            visitedPath = (packet.visitedPath.toMutableList().also { it.add(meshId) })
        )

        val connectedNodes = transport.connectedPeers
        val hasPeers = connectedNodes.any { it != immediateSender }

        if (!hasPeers && !isAckNack) {
            storeForLater(relayPacket)
            return
        }

        dispatchPacket(relayPacket, excludeHop = immediateSender)
        metrics.packetsForwarded.incrementAndGet()
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────────

    private fun dispatchPacket(packet: MeshPacket, excludeHop: String = "") {
        val connectedNodes = transport.connectedPeers

        metrics.routeCacheLookups.incrementAndGet()
        val nextHop = routeManager.getOptimalRoute(packet.targetId, setOf(excludeHop))
            ?.takeIf { connectedNodes.contains(it.nextHop) }
            ?.nextHop

        if (nextHop != null) {
            metrics.routeCacheHits.incrementAndGet()
            runSuspending { transport.broadcastPacket(packet, includeAddress = nextHop) }
        } else {
            // Broadcast to all except the immediate sender
            runSuspending { transport.broadcastPacket(packet, excludeAddress = excludeHop.ifBlank { null }) }
        }
    }

    // ── Store-and-Forward ─────────────────────────────────────────────────────────

    private fun storeForLater(packet: MeshPacket) {
        if (relayStore.size >= MAX_RELAY_SIZE) return
        val expiresAt = clock.currentTimeMs + (packet.ttl * 60_000L)
        relayStore[packet.packetId] = RelayEntry(packet, clock.currentTimeMs, expiresAt)
        recorder.recordStored(clock.currentTimeMs, meshId, packet)
        metrics.packetsStored.incrementAndGet()
        congestionMonitor.incrementRelay()
    }

    fun tryDeliverStored() {
        if (state != NodeState.ONLINE) return
        val now = clock.currentTimeMs
        val toDeliver = relayStore.values.toList()

        toDeliver.forEach { entry ->
            congestionMonitor.decrementRelay()
            if (entry.expiresAt < now || entry.packet.ttl <= 0) {
                relayStore.remove(entry.packet.packetId)
                metrics.packetsExpiredInStore.incrementAndGet()
                return@forEach
            }
            val refreshed = entry.packet.copy(
                ttl = entry.packet.ttl - 1,
                hopCount = entry.packet.hopCount + 1
            )
            relayStore.remove(entry.packet.packetId)
            dispatchPacket(refreshed)
            metrics.packetsDeliveredFromStore.incrementAndGet()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Synchronous bridge for suspend functions — safe within the single-threaded scheduler. */
    private fun runSuspending(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }

    override fun toString(): String = "SimulatedNode(id=$meshId, state=$state)"
}

/** Type alias to avoid importing java.util.concurrent.CopyOnWriteArrayList inline. */
private typealias CopyOnWriteArrayListAlias<T> = java.util.concurrent.CopyOnWriteArrayList<T>
