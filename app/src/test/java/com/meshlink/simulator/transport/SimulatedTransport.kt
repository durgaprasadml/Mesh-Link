package com.meshlink.simulator.transport

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.MeshError
import com.meshlink.domain.transport.Transport
import com.meshlink.simulator.core.SimulatedClock
import com.meshlink.simulator.core.SimulationScheduler
import com.meshlink.simulator.metrics.NetworkRecorder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process virtual transport implementing the production [Transport] interface.
 *
 * Packet delivery is fully deterministic:
 * - A [link] is looked up between sender and each target peer.
 * - If [link.isEnabled] is false the packet is silently dropped (disabled link).
 * - [TransportConfig.shouldDrop] is evaluated for random packet loss.
 * - [TransportConfig.shouldCorrupt] optionally flips a payload byte.
 * - Delivery is scheduled via [SimulationScheduler.scheduleAfter] using the link's
 *   sampled latency — no real `delay()` is used.
 *
 * Nodes register themselves as delivery targets via [registerNodeDelivery].
 *
 * Thread Safety: [connectedPeers] is a [ConcurrentHashMap] key set. The scheduler
 * is single-threaded by design.
 *
 * @param nodeId     The mesh ID of the node that owns this transport.
 * @param clock      Shared virtual clock.
 * @param scheduler  Shared virtual event scheduler.
 * @param recorder   Shared network recorder for packet event logging.
 * @param seed       Random seed for reproducible loss/latency decisions.
 */
class SimulatedTransport(
    val nodeId: String,
    private val clock: SimulatedClock,
    private val scheduler: SimulationScheduler,
    private val recorder: NetworkRecorder,
    seed: Long = 42L
) : Transport {

    private val random = Random(seed)

    // ── Incoming packet flow (consumed by the owning SimulatedNode) ──────────────
    private val _incomingPackets =
        MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 500)
    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> =
        _incomingPackets.asSharedFlow()

    // ── Connected peers and outbound links ────────────────────────────────────────
    private val _connectedPeers = ConcurrentHashMap.newKeySet<String>()
    override val connectedPeers: Set<String> get() = _connectedPeers.filter { links[it]?.isEnabled == true }.toSet()

    /** Links keyed by target node ID. */
    private val links = ConcurrentHashMap<String, Link>()

    /** Delivery lambdas registered by peer nodes. */
    private val deliveryTargets = ConcurrentHashMap<String, (sender: String, packet: MeshPacket) -> Unit>()

    // ── Link management ───────────────────────────────────────────────────────────

    /**
     * Adds an outgoing [link] to a peer node.
     * Also registers [peerId] as a connected peer.
     */
    fun addLink(link: Link) {
        require(link.fromNodeId == nodeId) {
            "Link.fromNodeId must match this transport's nodeId ($nodeId)"
        }
        links[link.toNodeId] = link
        _connectedPeers.add(link.toNodeId)
    }

    /** Removes the link to [peerId] and disconnects the peer. */
    fun removeLink(peerId: String) {
        links.remove(peerId)
        _connectedPeers.remove(peerId)
    }

    /**
     * Registers a delivery callback for incoming packets from the peer identified
     * by [fromNodeId]. The callback is invoked by the scheduler when a packet arrives.
     */
    fun registerNodeDelivery(fromNodeId: String, callback: (sender: String, packet: MeshPacket) -> Unit) {
        deliveryTargets[fromNodeId] = callback
    }

    // ── Transport interface ───────────────────────────────────────────────────────

    override suspend fun sendPacket(packet: MeshPacket): MeshResult<Unit> {
        return try {
            dispatchToTarget(packet.targetId, packet)
            MeshResult.Success(Unit)
        } catch (e: Exception) {
            MeshResult.Error(MeshError.TransportError("Send failed: ${e.message}"))
        }
    }

    override suspend fun broadcastPacket(
        packet: MeshPacket,
        excludeAddress: String?,
        includeAddress: String?
    ): MeshResult<Unit> {
        val targets = if (includeAddress != null) {
            setOf(includeAddress)
        } else {
            _connectedPeers.filter { it != excludeAddress }.toSet()
        }
        targets.forEach { peerId -> dispatchToTarget(peerId, packet) }
        return MeshResult.Success(Unit)
    }

    override suspend fun connectToPeer(peerId: String): MeshResult<Unit> {
        _connectedPeers.add(peerId)
        return MeshResult.Success(Unit)
    }

    // Deprecated overrides — delegate to new APIs
    @Deprecated("Use sendPacket", ReplaceWith("sendPacket(packet)"))
    override suspend fun send(packet: MeshPacket) { sendPacket(packet) }

    @Deprecated("Use broadcastPacket", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        broadcastPacket(packet, excludeAddress, includeAddress)
    }

    @Deprecated("Use connectToPeer", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) { connectToPeer(peerId) }

    // ── Internal dispatch ─────────────────────────────────────────────────────────

    private fun dispatchToTarget(targetNodeId: String, packet: MeshPacket) {
        val link = links[targetNodeId]

        // No link = target not directly connected
        if (link == null) {
            recorder.recordDrop(
                virtualTimeMs = clock.currentTimeMs,
                nodeId = nodeId,
                packet = packet,
                reason = NetworkRecorder.DropReason.NO_ROUTE
            )
            return
        }

        // Link disabled (partition / disconnection)
        if (!link.isEnabled) {
            recorder.recordDrop(
                virtualTimeMs = clock.currentTimeMs,
                nodeId = nodeId,
                packet = packet,
                reason = NetworkRecorder.DropReason.LINK_DISABLED
            )
            return
        }

        // Packet loss
        if (link.config.shouldDrop(random)) {
            recorder.recordDrop(
                virtualTimeMs = clock.currentTimeMs,
                nodeId = nodeId,
                packet = packet,
                reason = NetworkRecorder.DropReason.PACKET_LOSS
            )
            return
        }

        // Corruption
        val deliveredPacket = if (link.config.shouldCorrupt(random)) {
            corruptPayload(packet)
        } else {
            packet
        }

        val latency = link.config.sampleLatency(random)
        val sendTimeMs = clock.currentTimeMs

        recorder.recordSent(
            virtualTimeMs = sendTimeMs,
            nodeId = nodeId,
            packet = deliveredPacket,
            toNodeId = targetNodeId
        )

        // Schedule delivery at (now + latency)
        scheduler.scheduleAfter(latency) {
            val callback = deliveryTargets[targetNodeId]
            if (callback != null) {
                recorder.recordReceived(
                    virtualTimeMs = clock.currentTimeMs,
                    nodeId = targetNodeId,
                    packet = deliveredPacket,
                    fromNodeId = nodeId
                )
                callback(nodeId, deliveredPacket)
            } else {
                recorder.recordDrop(
                    virtualTimeMs = clock.currentTimeMs,
                    nodeId = nodeId,
                    packet = deliveredPacket,
                    reason = NetworkRecorder.DropReason.NO_DELIVERY_HANDLER
                )
            }
        }
    }

    /** Simulates payload corruption by flipping the first byte. */
    private fun corruptPayload(packet: MeshPacket): MeshPacket {
        val corrupted = if (packet.payload.isNotEmpty()) {
            val bytes = packet.payload.toByteArray(Charsets.UTF_8)
            bytes[0] = (bytes[0].toInt() xor 0xFF).toByte()
            String(bytes, Charsets.UTF_8) + "\u0000CORRUPTED"
        } else {
            "\u0000CORRUPTED"
        }
        return packet.copy(payload = corrupted)
    }

    // ── Introspection ─────────────────────────────────────────────────────────────

    /** Returns all outbound links from this node. */
    fun allLinks(): Map<String, Link> = links.toMap()

    override fun toString(): String =
        "SimulatedTransport(node=$nodeId, peers=$_connectedPeers)"
}
