package com.meshlink.simulator.metrics

import com.meshlink.domain.model.MeshPacket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Records every packet event during a simulation run for post-mortem debugging
 * and test assertions.
 *
 * Each packet event captures the virtual time, event type, node IDs, packet
 * metadata, hop count, TTL, trace ID (first element of `visitedPath` prefixed
 * with `"trace:"`), and — for drops — the reason.
 *
 * Thread Safety: Uses [CopyOnWriteArrayList] for safe concurrent appends.
 *
 * Usage:
 * ```kotlin
 * val recorder = NetworkRecorder()
 * // ... run simulation ...
 * val trace = recorder.getTraceForPacket("trace:abc")
 * recorder.printTrace("trace:abc")
 * recorder.exportToJson()
 * ```
 */
class NetworkRecorder {

    // ── Event Types ───────────────────────────────────────────────────────────────

    /** The type of packet lifecycle event. */
    enum class EventType {
        SENT,
        RECEIVED,
        FORWARDED,
        DROPPED,
        STORED,          // Entered store-and-forward queue
        DELIVERED        // Final delivery to destination node
    }

    /** Why a packet was dropped. */
    enum class DropReason {
        TTL_EXPIRED,
        DUPLICATE,
        LOOP_DETECTED,
        TRUST_BLOCKED,
        ENCRYPTION_FAILED,
        PACKET_LOSS,       // Random loss by transport
        LINK_DISABLED,
        QUEUE_FULL,
        NO_ROUTE,
        NO_DELIVERY_HANDLER,
        CORRUPTED
    }

    /**
     * An immutable snapshot of one packet event at a specific virtual time.
     *
     * @param traceId  The trace ID embedded in `packet.visitedPath[0]`
     *   (format: `"trace:<uuid>"`). Empty string if no trace was added.
     */
    data class PacketEvent(
        val virtualTimeMs: Long,
        val eventType: EventType,
        val nodeId: String,
        val packetId: String,
        val traceId: String,
        val fromNodeId: String?,
        val toNodeId: String?,
        val hopCount: Int,
        val ttl: Int,
        val packetType: String,
        val dropReason: DropReason? = null
    )

    // ── Storage ───────────────────────────────────────────────────────────────────

    private val events = java.util.concurrent.ConcurrentLinkedQueue<PacketEvent>()

    // ── Recording API ─────────────────────────────────────────────────────────────

    fun recordSent(virtualTimeMs: Long, nodeId: String, packet: MeshPacket, toNodeId: String) {
        events.add(PacketEvent(
            virtualTimeMs = virtualTimeMs,
            eventType = EventType.SENT,
            nodeId = nodeId,
            packetId = packet.packetId,
            traceId = extractTraceId(packet),
            fromNodeId = nodeId,
            toNodeId = toNodeId,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            packetType = packet.type.name
        ))
    }

    fun recordReceived(virtualTimeMs: Long, nodeId: String, packet: MeshPacket, fromNodeId: String) {
        events.add(PacketEvent(
            virtualTimeMs = virtualTimeMs,
            eventType = EventType.RECEIVED,
            nodeId = nodeId,
            packetId = packet.packetId,
            traceId = extractTraceId(packet),
            fromNodeId = fromNodeId,
            toNodeId = nodeId,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            packetType = packet.type.name
        ))
    }

    fun recordForwarded(virtualTimeMs: Long, nodeId: String, packet: MeshPacket, toNodeId: String) {
        events.add(PacketEvent(
            virtualTimeMs = virtualTimeMs,
            eventType = EventType.FORWARDED,
            nodeId = nodeId,
            packetId = packet.packetId,
            traceId = extractTraceId(packet),
            fromNodeId = nodeId,
            toNodeId = toNodeId,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            packetType = packet.type.name
        ))
    }

    fun recordDrop(
        virtualTimeMs: Long,
        nodeId: String,
        packet: MeshPacket,
        reason: DropReason,
        fromNodeId: String? = null
    ) {
        events.add(PacketEvent(
            virtualTimeMs = virtualTimeMs,
            eventType = EventType.DROPPED,
            nodeId = nodeId,
            packetId = packet.packetId,
            traceId = extractTraceId(packet),
            fromNodeId = fromNodeId,
            toNodeId = null,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            packetType = packet.type.name,
            dropReason = reason
        ))
    }

    fun recordStored(virtualTimeMs: Long, nodeId: String, packet: MeshPacket) {
        events.add(PacketEvent(
            virtualTimeMs = virtualTimeMs,
            eventType = EventType.STORED,
            nodeId = nodeId,
            packetId = packet.packetId,
            traceId = extractTraceId(packet),
            fromNodeId = null,
            toNodeId = packet.targetId,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            packetType = packet.type.name
        ))
    }

    fun recordDelivered(virtualTimeMs: Long, nodeId: String, packet: MeshPacket, fromNodeId: String?) {
        events.add(PacketEvent(
            virtualTimeMs = virtualTimeMs,
            eventType = EventType.DELIVERED,
            nodeId = nodeId,
            packetId = packet.packetId,
            traceId = extractTraceId(packet),
            fromNodeId = fromNodeId,
            toNodeId = nodeId,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            packetType = packet.type.name
        ))
    }

    // ── Query API ─────────────────────────────────────────────────────────────────

    /** All recorded events in chronological order. */
    fun allEvents(): List<PacketEvent> = events.toList()

    /** All events for a specific packet ID. */
    fun eventsForPacket(packetId: String): List<PacketEvent> =
        events.filter { it.packetId == packetId }

    /**
     * Full hop trace for a trace ID — includes SENT, FORWARDED, RECEIVED, DROPPED events.
     * @param traceId The `"trace:<uuid>"` string embedded as `visitedPath[0]`.
     */
    fun getTraceForPacket(traceId: String): List<PacketEvent> =
        events.filter { it.traceId == traceId }.sortedBy { it.virtualTimeMs }

    /** All events where the packet was finally delivered to its destination. */
    fun getDeliveredEvents(): List<PacketEvent> =
        events.filter { it.eventType == EventType.DELIVERED }

    /** All drop events, optionally filtered by reason. */
    fun getDroppedPackets(reason: DropReason? = null): List<PacketEvent> =
        events.filter {
            it.eventType == EventType.DROPPED && (reason == null || it.dropReason == reason)
        }

    /** Whether a specific packet was delivered to [toNodeId]. */
    fun wasDeliveredTo(packetId: String, toNodeId: String): Boolean =
        events.any { it.packetId == packetId && it.eventType == EventType.DELIVERED && it.nodeId == toNodeId }

    /** How many times a packet was delivered (should be 1 for dedup-correct operation). */
    fun deliveryCount(packetId: String): Int =
        events.count { it.packetId == packetId && it.eventType == EventType.DELIVERED }

    /** Distinct node IDs that forwarded or received a given packet. */
    fun visitedNodes(packetId: String): List<String> =
        events.filter {
            it.packetId == packetId &&
            it.eventType in setOf(EventType.RECEIVED, EventType.FORWARDED, EventType.DELIVERED)
        }.map { it.nodeId }.distinct()

    // ── Export ────────────────────────────────────────────────────────────────────

    /** Exports all events as a JSON array string. */
    fun exportToJson(): String = buildString {
        appendLine("[")
        val eventList = events.toList()
        eventList.forEachIndexed { idx, e ->
            val comma = if (idx < eventList.size - 1) "," else ""
            appendLine(
                "  {" +
                "\"t\":${e.virtualTimeMs}," +
                "\"type\":\"${e.eventType}\"," +
                "\"node\":\"${e.nodeId}\"," +
                "\"packetId\":\"${e.packetId}\"," +
                "\"traceId\":\"${e.traceId}\"," +
                "\"from\":${e.fromNodeId?.let { "\"$it\"" } ?: "null"}," +
                "\"to\":${e.toNodeId?.let { "\"$it\"" } ?: "null"}," +
                "\"hopCount\":${e.hopCount}," +
                "\"ttl\":${e.ttl}," +
                "\"packetType\":\"${e.packetType}\"," +
                "\"dropReason\":${e.dropReason?.let { "\"$it\"" } ?: "null"}" +
                "}$comma"
            )
        }
        append("]")
    }

    /**
     * Prints a human-readable hop-by-hop trace for [traceId] to stdout.
     * Useful for diagnosing failed test assertions.
     */
    fun printTrace(traceId: String) {
        val trace = getTraceForPacket(traceId)
        println("=== Packet Trace: $traceId (${trace.size} events) ===")
        trace.forEach { e ->
            val arrow = when (e.eventType) {
                EventType.SENT      -> "↗"
                EventType.RECEIVED  -> "↘"
                EventType.FORWARDED -> "→"
                EventType.DROPPED   -> "✗ [${e.dropReason}]"
                EventType.STORED    -> "📦"
                EventType.DELIVERED -> "✓"
            }
            println("  t=${e.virtualTimeMs}ms  [${e.nodeId}]  $arrow  " +
                    "hop=${e.hopCount} ttl=${e.ttl} type=${e.packetType}")
        }
        println("=== End Trace ===")
    }

    /** Clears all recorded events. Call between test cases to avoid cross-contamination. */
    fun reset() { events.clear() }

    /** Total number of recorded events. */
    fun eventCount(): Int = events.size

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun extractTraceId(packet: MeshPacket): String =
        packet.visitedPath.firstOrNull { it.startsWith("trace:") } ?: ""
}
