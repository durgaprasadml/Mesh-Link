package com.meshlink.simulator.assertions

import com.meshlink.simulator.core.SimulationEnvironment
import com.meshlink.simulator.metrics.NetworkRecorder
import com.meshlink.simulator.metrics.NetworkRecorder.EventType
import com.meshlink.simulator.metrics.SimulationReport
import com.meshlink.simulator.security.SimulatedSecurityLayer
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Fluent assertion helpers for mesh simulation test validation.
 *
 * All assertions throw [AssertionError] on failure with descriptive messages
 * including the relevant packet trace for debugging.
 *
 * Extended assertions (Req 6):
 * - Hop count validation
 * - Visited path validation
 * - Duplicate suppression
 * - Loop prevention
 * - Delivery assertions
 * - Security assertions
 * - Metrics assertions
 */
object MeshAssertions {

    // ── Delivery Assertions ───────────────────────────────────────────────────────

    /**
     * Asserts that [packetId] was delivered to [toNodeId].
     */
    fun assertDelivered(recorder: NetworkRecorder, packetId: String, toNodeId: String) {
        assertTrue(
            recorder.wasDeliveredTo(packetId, toNodeId),
            "Expected packet '$packetId' to be delivered to '$toNodeId' but it was not.\n" +
            buildDropInfo(recorder, packetId)
        )
    }

    /**
     * Asserts that [packetId] was NOT delivered to any node.
     */
    fun assertNotDelivered(recorder: NetworkRecorder, packetId: String) {
        val delivered = recorder.getDeliveredEvents().filter { it.packetId == packetId }
        assertTrue(
            delivered.isEmpty(),
            "Expected packet '$packetId' to NOT be delivered, but it was delivered to: " +
            delivered.map { it.nodeId }
        )
    }

    /**
     * Asserts that [packetId] was delivered to [toNodeId] and that the final delivery
     * happened within [maxMs] virtual milliseconds from the first SENT event.
     */
    fun assertDeliveredWithinVirtualMs(
        recorder: NetworkRecorder,
        packetId: String,
        toNodeId: String,
        maxMs: Long
    ) {
        val sentEvent = recorder.eventsForPacket(packetId)
            .firstOrNull { it.eventType == EventType.SENT }
            ?: fail("No SENT event found for packet '$packetId'")

        val deliveredEvent = recorder.eventsForPacket(packetId)
            .firstOrNull { it.eventType == EventType.DELIVERED && it.nodeId == toNodeId }
            ?: fail("Packet '$packetId' was not delivered to '$toNodeId'")

        val latency = deliveredEvent.virtualTimeMs - sentEvent.virtualTimeMs
        assertTrue(
            latency <= maxMs,
            "Packet '$packetId' delivery latency was ${latency}ms, expected <= ${maxMs}ms"
        )
    }

    // ── Hop Count Assertions ──────────────────────────────────────────────────────

    /**
     * Asserts that the packet identified by [traceId] was delivered with exactly
     * [expectedHops] hops.
     */
    fun assertHopCount(recorder: NetworkRecorder, traceId: String, expectedHops: Int) {
        val deliveredEvent = recorder.getTraceForPacket(traceId)
            .firstOrNull { it.eventType == EventType.DELIVERED }
            ?: fail("No DELIVERED event found for traceId '$traceId'.\n${buildTrace(recorder, traceId)}")

        assertEquals(
            expectedHops, deliveredEvent.hopCount,
            "Expected hop count $expectedHops but got ${deliveredEvent.hopCount} for trace '$traceId'.\n" +
            buildTrace(recorder, traceId)
        )
    }

    /**
     * Asserts that the packet identified by [traceId] was delivered with at most
     * [maxHops] hops.
     */
    fun assertHopCountAtMost(recorder: NetworkRecorder, traceId: String, maxHops: Int) {
        val deliveredEvent = recorder.getTraceForPacket(traceId)
            .firstOrNull { it.eventType == EventType.DELIVERED } ?: return
        assertTrue(
            deliveredEvent.hopCount <= maxHops,
            "Packet hop count ${deliveredEvent.hopCount} exceeded max $maxHops for trace '$traceId'"
        )
    }

    // ── Visited Path Assertions ───────────────────────────────────────────────────

    /**
     * Asserts that the packet identified by [traceId] visited exactly [expectedPath]
     * nodes (in order), derived from the sequence of FORWARDED + DELIVERED events.
     */
    fun assertVisitedPath(recorder: NetworkRecorder, traceId: String, expectedPath: List<String>) {
        val visitedNodes = recorder.getTraceForPacket(traceId)
            .filter { it.eventType in setOf(EventType.RECEIVED, EventType.FORWARDED, EventType.DELIVERED) }
            .map { it.nodeId }
            .distinct()

        assertEquals(
            expectedPath, visitedNodes,
            "Expected path $expectedPath but got $visitedNodes for trace '$traceId'.\n" +
            buildTrace(recorder, traceId)
        )
    }

    /**
     * Asserts that [expectedNode] appears somewhere in the hop path of [traceId].
     */
    fun assertNodeVisited(recorder: NetworkRecorder, traceId: String, expectedNode: String) {
        val visited = recorder.visitedNodes(findPacketIdByTrace(recorder, traceId))
        assertTrue(
            visited.contains(expectedNode),
            "Expected node '$expectedNode' in path $visited for trace '$traceId'"
        )
    }

    // ── Duplicate Suppression Assertions ─────────────────────────────────────────

    /**
     * Asserts that [packetId] was delivered exactly once across the entire simulation.
     */
    fun assertDeliveredOnce(recorder: NetworkRecorder, packetId: String) {
        val count = recorder.deliveryCount(packetId)
        assertEquals(1, count, "Packet '$packetId' was delivered $count times (expected exactly 1)")
    }

    /**
     * Asserts that no packet in the entire simulation was delivered more than once.
     */
    fun assertNoDuplicateDelivery(recorder: NetworkRecorder) {
        val duplicates = recorder.getDeliveredEvents()
            .groupBy { it.packetId }
            .filter { (_, events) -> 
                events.groupBy { it.nodeId }.any { (_, nodeEvents) -> nodeEvents.size > 1 }
            }

        assertTrue(
            duplicates.isEmpty(),
            "Duplicate deliveries detected (same node received same packet): ${
                duplicates.map { (k, v) -> "$k: ${v.groupBy { it.nodeId }.filter { it.value.size > 1 }.map { "${it.key}x${it.value.size}" }}" }
            }"
        )
    }

    // ── Loop Prevention Assertions ────────────────────────────────────────────────

    /**
     * Asserts that the packet identified by [traceId] did NOT form a routing loop
     * (no node appears more than once in the forwarded path).
     */
    fun assertNoLoop(recorder: NetworkRecorder, traceId: String) {
        val forwardedNodes = recorder.getTraceForPacket(traceId)
            .filter { it.eventType == EventType.FORWARDED }
            .map { it.nodeId }

        val duplicateNodes = forwardedNodes.groupBy { it }.filter { (_, v) -> v.size > 1 }.keys

        assertTrue(
            duplicateNodes.isEmpty(),
            "Routing loop detected! Nodes forwarded packet multiple times: $duplicateNodes for trace '$traceId'.\n" +
            buildTrace(recorder, traceId)
        )
    }

    /**
     * Asserts that the packet identified by [traceId] was dropped due to TTL expiry
     * (not due to a loop) — verifying TTL-bounded termination rather than infinite looping.
     */
    fun assertTtlExpiredBeforeLoop(recorder: NetworkRecorder, traceId: String) {
        val drops = recorder.getTraceForPacket(traceId)
            .filter { it.eventType == EventType.DROPPED }

        val hadTtlDrop = drops.any { it.dropReason == NetworkRecorder.DropReason.TTL_EXPIRED }
        val hadLoopDrop = drops.any { it.dropReason == NetworkRecorder.DropReason.LOOP_DETECTED }

        assertFalse(
            hadLoopDrop && !hadTtlDrop,
            "Expected TTL expiry to prevent loop but got loop-detected drop for trace '$traceId'"
        )
        assertTrue(
            hadTtlDrop || hadLoopDrop,
            "Expected packet to be dropped (TTL or loop) but no drop was recorded for trace '$traceId'"
        )
    }

    // ── Security Assertions ───────────────────────────────────────────────────────

    /**
     * Asserts that replaying [ciphertext] to [secLayer] for [peerId] returns null (rejected).
     */
    fun assertReplayRejected(secLayer: SimulatedSecurityLayer, peerId: String, ciphertext: String) {
        val result = secLayer.decrypt(peerId, ciphertext)
        assertTrue(
            result == null,
            "Expected replay to be rejected (null decrypt) but decryption succeeded for peer '$peerId'"
        )
    }

    /**
     * Asserts that a packet with [packetId] was dropped due to encryption failure.
     */
    fun assertEncryptionFailureDrop(recorder: NetworkRecorder, packetId: String) {
        val dropped = recorder.getDroppedPackets(NetworkRecorder.DropReason.ENCRYPTION_FAILED)
        assertTrue(
            dropped.any { it.packetId == packetId },
            "Expected packet '$packetId' to be dropped due to ENCRYPTION_FAILED but it was not.\n" +
            buildDropInfo(recorder, packetId)
        )
    }

    /**
     * Asserts that AES-GCM encrypt → decrypt round-trip produces [expectedPlaintext].
     */
    fun assertEncryptionRoundTrip(secLayer: SimulatedSecurityLayer, peerId: String, plaintext: String) {
        secLayer.establishSession(peerId)
        val ct = secLayer.encrypt(peerId, plaintext)
        val decrypted = secLayer.decrypt(peerId, ct)
        assertEquals(plaintext, decrypted, "Encryption round-trip failed for peer '$peerId'")
    }

    // ── Metrics Assertions ────────────────────────────────────────────────────────

    /**
     * Asserts that the aggregate delivery success rate is at least [minRate] (0.0..1.0).
     */
    fun assertDeliveryRate(report: SimulationReport, minRate: Float) {
        assertTrue(
            report.deliverySuccessRate >= minRate,
            "Delivery rate ${report.deliverySuccessRate} < minimum $minRate\n$report"
        )
    }

    /**
     * Asserts that average delivery latency is below [maxMs] virtual milliseconds.
     */
    fun assertAverageLatencyBelow(report: SimulationReport, maxMs: Double) {
        assertTrue(
            report.averageDeliveryLatencyMs <= maxMs,
            "Average latency ${report.averageDeliveryLatencyMs}ms exceeded max $maxMs ms"
        )
    }

    /**
     * Asserts that no node's queue is stuck (non-zero size after simulation completes).
     */
    fun assertNoDeadlock(env: SimulationEnvironment) {
        val stuck = env.nodes.filter { it.isOnline && it.queueOptimizer.size() > 0 }
        assertTrue(
            stuck.isEmpty(),
            "Deadlock detected! Nodes with non-empty queues after simulation: ${stuck.map { it.meshId }}"
        )
    }

    /**
     * Asserts that duplicate suppression was active (at least one duplicate was suppressed).
     */
    fun assertDuplicateSuppression(report: SimulationReport) {
        assertTrue(
            report.totalDuplicatesSuppressed > 0,
            "Expected at least one duplicate to be suppressed but got 0"
        )
    }

    /**
     * Asserts that at least one retransmission occurred.
     */
    fun assertRetransmissionsOccurred(report: SimulationReport) {
        assertTrue(
            report.totalRetransmissions > 0,
            "Expected at least one retransmission but count was 0"
        )
    }

    /**
     * Asserts that average encryption latency is below [maxNs] nanoseconds.
     */
    fun assertEncryptionLatencyBelow(report: SimulationReport, maxNs: Long) {
        assertTrue(
            report.avgEncryptionLatencyNanos <= maxNs,
            "Avg encryption latency ${report.avgEncryptionLatencyNanos}ns exceeded max $maxNs ns"
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun buildTrace(recorder: NetworkRecorder, traceId: String): String {
        val events = recorder.getTraceForPacket(traceId)
        return "Trace events for '$traceId' (${events.size} events):\n" +
            events.joinToString("\n") { "  t=${it.virtualTimeMs}ms [${it.nodeId}] ${it.eventType} drop=${it.dropReason}" }
    }

    private fun buildDropInfo(recorder: NetworkRecorder, packetId: String): String {
        val drops = recorder.getDroppedPackets().filter { it.packetId == packetId }
        return if (drops.isEmpty()) "No drop events recorded for this packet."
        else "Drop events: " + drops.joinToString(", ") { "[${it.nodeId}: ${it.dropReason}]" }
    }

    private fun findPacketIdByTrace(recorder: NetworkRecorder, traceId: String): String =
        recorder.getTraceForPacket(traceId).firstOrNull()?.packetId ?: traceId

    private fun fail(message: String): Nothing = throw AssertionError(message)
}
