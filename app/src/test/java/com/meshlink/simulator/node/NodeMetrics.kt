package com.meshlink.simulator.node

import java.util.concurrent.atomic.AtomicLong

/**
 * Per-node metrics counters for the mesh simulation framework.
 *
 * Updated in-line by [SimulatedNode] during packet processing.
 * All counters are [AtomicLong] for safe concurrent access during
 * multi-threaded stress tests.
 *
 * Extended metrics (Req 8) include:
 * - Retransmissions
 * - Congestion events
 * - Queue wait time (running sum + sample count → derive average)
 * - Route cache lookups and hits
 * - Duplicate cache lookups and hits
 * - Encryption latency (nanoseconds)
 */
class NodeMetrics(val nodeId: String) {

    // ── Delivery Counters ─────────────────────────────────────────────────────────
    val packetsSent = AtomicLong(0)
    val packetsReceived = AtomicLong(0)
    val packetsForwarded = AtomicLong(0)
    val packetsDropped = AtomicLong(0)

    // ── Deduplication ─────────────────────────────────────────────────────────────
    val duplicatesSuppressed = AtomicLong(0)
    val duplicateCacheLookups = AtomicLong(0)
    val duplicateCacheHits = AtomicLong(0)

    // ── TTL / Loop Prevention ─────────────────────────────────────────────────────
    val ttlExpirations = AtomicLong(0)
    val loopsDetected = AtomicLong(0)

    // ── Store-and-Forward ─────────────────────────────────────────────────────────
    val packetsStored = AtomicLong(0)
    val packetsDeliveredFromStore = AtomicLong(0)
    val packetsExpiredInStore = AtomicLong(0)

    // ── Retransmissions (Req 8) ───────────────────────────────────────────────────
    val retransmissions = AtomicLong(0)

    // ── Congestion Events (Req 8) ─────────────────────────────────────────────────
    val congestionEvents = AtomicLong(0)

    // ── Queue Wait Time (Req 8) ───────────────────────────────────────────────────
    /** Sum of all queue wait times in virtual milliseconds. */
    val queueWaitTimeSumMs = AtomicLong(0)
    /** Count of queue wait time samples. */
    val queueWaitTimeSamples = AtomicLong(0)

    /** Average queue wait time in virtual milliseconds. */
    val avgQueueWaitMs: Double
        get() {
            val samples = queueWaitTimeSamples.get()
            return if (samples == 0L) 0.0 else queueWaitTimeSumMs.get().toDouble() / samples
        }

    // ── Route Cache (Req 8) ───────────────────────────────────────────────────────
    val routeCacheLookups = AtomicLong(0)
    val routeCacheHits = AtomicLong(0)

    val routeCacheHitRatio: Double
        get() {
            val lookups = routeCacheLookups.get()
            return if (lookups == 0L) 0.0 else routeCacheHits.get().toDouble() / lookups
        }

    // ── Encryption Latency (Req 8) ────────────────────────────────────────────────
    val encryptionLatencyNanosSamples = mutableListOf<Long>()
    private val encryptionLock = Any()

    fun recordEncryptionLatency(nanos: Long) {
        synchronized(encryptionLock) {
            encryptionLatencyNanosSamples.add(nanos)
        }
    }

    val avgEncryptionLatencyNanos: Double
        get() = synchronized(encryptionLock) {
            if (encryptionLatencyNanosSamples.isEmpty()) 0.0
            else encryptionLatencyNanosSamples.average()
        }

    val p99EncryptionLatencyNanos: Long
        get() = synchronized(encryptionLock) {
            if (encryptionLatencyNanosSamples.isEmpty()) 0L
            else {
                val sorted = encryptionLatencyNanosSamples.sorted()
                sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.size - 1)]
            }
        }

    // ── Hop Count Tracking ────────────────────────────────────────────────────────
    val hopCountSum = AtomicLong(0)
    val hopCountSamples = AtomicLong(0)

    val avgHopCount: Double
        get() {
            val samples = hopCountSamples.get()
            return if (samples == 0L) 0.0 else hopCountSum.get().toDouble() / samples
        }

    // ── Latency Tracking (virtual ms) ────────────────────────────────────────────
    val deliveryLatencySumMs = AtomicLong(0)
    val deliveryLatencySamples = AtomicLong(0)

    val avgDeliveryLatencyMs: Double
        get() {
            val samples = deliveryLatencySamples.get()
            return if (samples == 0L) 0.0 else deliveryLatencySumMs.get().toDouble() / samples
        }

    // ── Reset ─────────────────────────────────────────────────────────────────────
    fun reset() {
        packetsSent.set(0); packetsReceived.set(0); packetsForwarded.set(0)
        packetsDropped.set(0); duplicatesSuppressed.set(0); duplicateCacheLookups.set(0)
        duplicateCacheHits.set(0); ttlExpirations.set(0); loopsDetected.set(0)
        packetsStored.set(0); packetsDeliveredFromStore.set(0); packetsExpiredInStore.set(0)
        retransmissions.set(0); congestionEvents.set(0)
        queueWaitTimeSumMs.set(0); queueWaitTimeSamples.set(0)
        routeCacheLookups.set(0); routeCacheHits.set(0)
        hopCountSum.set(0); hopCountSamples.set(0)
        deliveryLatencySumMs.set(0); deliveryLatencySamples.set(0)
        synchronized(encryptionLock) { encryptionLatencyNanosSamples.clear() }
    }

    override fun toString(): String = buildString {
        appendLine("NodeMetrics[$nodeId]:")
        appendLine("  sent=${ packetsSent.get() }  rcvd=${packetsReceived.get()}  fwd=${packetsForwarded.get()}  dropped=${packetsDropped.get()}")
        appendLine("  dups=${duplicatesSuppressed.get()}  ttlExp=${ttlExpirations.get()}  loops=${loopsDetected.get()}")
        appendLine("  stored=${packetsStored.get()}  sfDelivered=${packetsDeliveredFromStore.get()}")
        appendLine("  retx=${retransmissions.get()}  congestion=${congestionEvents.get()}")
        appendLine("  avgQueueWait=${avgQueueWaitMs.toInt()}ms  routeHitRatio=${"%.2f".format(routeCacheHitRatio)}")
        append  ("  avgEncryptNs=${avgEncryptionLatencyNanos.toLong()}  p99EncryptNs=${p99EncryptionLatencyNanos}")
    }
}
