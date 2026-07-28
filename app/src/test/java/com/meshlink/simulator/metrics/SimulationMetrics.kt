package com.meshlink.simulator.metrics

import com.meshlink.simulator.node.NodeMetrics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Aggregate simulation metrics collected across all nodes and the entire simulation run.
 *
 * Combines per-node [NodeMetrics] into network-wide statistics. Includes all
 * standard metrics plus Req 8 extended metrics:
 * - Retransmissions
 * - Congestion events
 * - Queue wait time (avg + p95)
 * - Route cache hit ratio
 * - Duplicate cache hit ratio
 * - Encryption latency (avg + p99)
 *
 * Usage:
 * ```kotlin
 * val metrics = SimulationMetrics()
 * // Register nodes
 * metrics.registerNode(nodeMetrics)
 * // After simulation
 * val report = metrics.generateReport()
 * println(report)
 * ```
 */
class SimulationMetrics {

    private val nodeMetricsMap = ConcurrentHashMap<String, NodeMetrics>()

    // ── Simulation-wide event counters ────────────────────────────────────────────

    /** Virtual milliseconds from topology change to first successful delivery. */
    val routeConvergenceTimeMs = AtomicLong(-1L)

    /** Number of global congestion state changes across all nodes. */
    private val globalCongestionEvents = AtomicLong(0)

    fun recordCongestionEvent() { globalCongestionEvents.incrementAndGet() }

    /** Virtual start time of the simulation (set on first [registerNode]). */
    var simulationStartMs: Long = 0L
    /** Virtual end time (set by [SimulationEnvironment]). */
    var simulationEndMs: Long = 0L

    // ── Node registration ─────────────────────────────────────────────────────────

    fun registerNode(nm: NodeMetrics) { nodeMetricsMap[nm.nodeId] = nm }

    fun nodeMetrics(nodeId: String): NodeMetrics? = nodeMetricsMap[nodeId]

    fun allNodeMetrics(): Collection<NodeMetrics> = nodeMetricsMap.values

    // ── Aggregate computations ────────────────────────────────────────────────────

    val totalPacketsSent: Long get() = nodeMetricsMap.values.sumOf { it.packetsSent.get() }
    val totalPacketsReceived: Long get() = nodeMetricsMap.values.sumOf { it.packetsReceived.get() }
    val totalPacketsForwarded: Long get() = nodeMetricsMap.values.sumOf { it.packetsForwarded.get() }
    val totalPacketsDropped: Long get() = nodeMetricsMap.values.sumOf { it.packetsDropped.get() }
    val totalDuplicatesSuppressed: Long get() = nodeMetricsMap.values.sumOf { it.duplicatesSuppressed.get() }
    val totalTtlExpirations: Long get() = nodeMetricsMap.values.sumOf { it.ttlExpirations.get() }
    val totalLoopsDetected: Long get() = nodeMetricsMap.values.sumOf { it.loopsDetected.get() }
    val totalPacketsStored: Long get() = nodeMetricsMap.values.sumOf { it.packetsStored.get() }
    val totalPacketsDeliveredFromStore: Long get() = nodeMetricsMap.values.sumOf { it.packetsDeliveredFromStore.get() }
    val totalRetransmissions: Long get() = nodeMetricsMap.values.sumOf { it.retransmissions.get() }

    /** Overall delivery success rate = delivered / (sent - SOS-broadcasts that could arrive multiple times). */
    val deliverySuccessRate: Float
        get() {
            val sent = totalPacketsSent
            val delivered = totalPacketsReceived
            return if (sent == 0L) 1.0f else delivered.toFloat() / sent.toFloat()
        }

    val averageHopCount: Double
        get() {
            val total = nodeMetricsMap.values.sumOf { it.hopCountSum.get() }
            val count = nodeMetricsMap.values.sumOf { it.hopCountSamples.get() }
            return if (count == 0L) 0.0 else total.toDouble() / count
        }

    val averageDeliveryLatencyMs: Double
        get() {
            val total = nodeMetricsMap.values.sumOf { it.deliveryLatencySumMs.get() }
            val count = nodeMetricsMap.values.sumOf { it.deliveryLatencySamples.get() }
            return if (count == 0L) 0.0 else total.toDouble() / count
        }

    val averageQueueWaitMs: Double
        get() {
            val total = nodeMetricsMap.values.sumOf { it.queueWaitTimeSumMs.get() }
            val count = nodeMetricsMap.values.sumOf { it.queueWaitTimeSamples.get() }
            return if (count == 0L) 0.0 else total.toDouble() / count
        }

    val aggregateRouteCacheHitRatio: Double
        get() {
            val hits = nodeMetricsMap.values.sumOf { it.routeCacheHits.get() }
            val lookups = nodeMetricsMap.values.sumOf { it.routeCacheLookups.get() }
            return if (lookups == 0L) 0.0 else hits.toDouble() / lookups
        }

    val aggregateDuplicateCacheHitRatio: Double
        get() {
            val hits = nodeMetricsMap.values.sumOf { it.duplicateCacheHits.get() }
            val lookups = nodeMetricsMap.values.sumOf { it.duplicateCacheLookups.get() }
            return if (lookups == 0L) 0.0 else hits.toDouble() / lookups
        }

    val avgEncryptionLatencyNanos: Double
        get() {
            val allSamples = nodeMetricsMap.values.flatMap {
                synchronized(it) { it.encryptionLatencyNanosSamples.toList() }
            }
            return if (allSamples.isEmpty()) 0.0 else allSamples.average()
        }

    val p99EncryptionLatencyNanos: Long
        get() {
            val allSamples = nodeMetricsMap.values.flatMap {
                synchronized(it) { it.encryptionLatencyNanosSamples.toList() }
            }.sorted()
            return if (allSamples.isEmpty()) 0L
            else allSamples[(allSamples.size * 0.99).toInt().coerceAtMost(allSamples.size - 1)]
        }

    val totalCongestionEvents: Long
        get() = globalCongestionEvents.get() +
                nodeMetricsMap.values.sumOf { it.congestionEvents.get() }

    // ── Report generation ─────────────────────────────────────────────────────────

    /** Generates a structured [SimulationReport] from current metrics. */
    fun generateReport(): SimulationReport = SimulationReport(
        totalPacketsSent = totalPacketsSent,
        totalPacketsReceived = totalPacketsReceived,
        totalPacketsForwarded = totalPacketsForwarded,
        totalPacketsDropped = totalPacketsDropped,
        deliverySuccessRate = deliverySuccessRate,
        averageHopCount = averageHopCount,
        averageDeliveryLatencyMs = averageDeliveryLatencyMs,
        totalDuplicatesSuppressed = totalDuplicatesSuppressed,
        duplicateCacheHitRatio = aggregateDuplicateCacheHitRatio,
        totalTtlExpirations = totalTtlExpirations,
        totalLoopsDetected = totalLoopsDetected,
        totalPacketsStored = totalPacketsStored,
        totalPacketsDeliveredFromStore = totalPacketsDeliveredFromStore,
        routeConvergenceTimeMs = routeConvergenceTimeMs.get(),
        totalRetransmissions = totalRetransmissions,
        totalCongestionEvents = totalCongestionEvents,
        averageQueueWaitMs = averageQueueWaitMs,
        routeCacheHitRatio = aggregateRouteCacheHitRatio,
        avgEncryptionLatencyNanos = avgEncryptionLatencyNanos,
        p99EncryptionLatencyNanos = p99EncryptionLatencyNanos,
        nodeCount = nodeMetricsMap.size,
        simulationDurationMs = simulationEndMs - simulationStartMs
    )

    /** Resets all node metric counters and simulation-wide counters. */
    fun reset() {
        nodeMetricsMap.values.forEach { it.reset() }
        routeConvergenceTimeMs.set(-1L)
        globalCongestionEvents.set(0L)
        simulationStartMs = 0L
        simulationEndMs = 0L
    }
}

/**
 * Immutable snapshot of all simulation metrics after a run.
 * Suitable for test assertions and pretty-printing.
 */
data class SimulationReport(
    val totalPacketsSent: Long,
    val totalPacketsReceived: Long,
    val totalPacketsForwarded: Long,
    val totalPacketsDropped: Long,
    val deliverySuccessRate: Float,
    val averageHopCount: Double,
    val averageDeliveryLatencyMs: Double,
    val totalDuplicatesSuppressed: Long,
    val duplicateCacheHitRatio: Double,
    val totalTtlExpirations: Long,
    val totalLoopsDetected: Long,
    val totalPacketsStored: Long,
    val totalPacketsDeliveredFromStore: Long,
    val routeConvergenceTimeMs: Long,
    // Req 8 extended
    val totalRetransmissions: Long,
    val totalCongestionEvents: Long,
    val averageQueueWaitMs: Double,
    val routeCacheHitRatio: Double,
    val avgEncryptionLatencyNanos: Double,
    val p99EncryptionLatencyNanos: Long,
    val nodeCount: Int,
    val simulationDurationMs: Long
) {
    override fun toString(): String = buildString {
        appendLine("╔══════════════════════════════════════════════╗")
        appendLine("║        SIMULATION REPORT                     ║")
        appendLine("╠══════════════════════════════════════════════╣")
        appendLine("║ Nodes: $nodeCount  Duration: ${simulationDurationMs}ms (virtual)")
        appendLine("║ ─── Delivery ────────────────────────────── ║")
        appendLine("║  Sent:           $totalPacketsSent")
        appendLine("║  Received:       $totalPacketsReceived")
        appendLine("║  Forwarded:      $totalPacketsForwarded")
        appendLine("║  Dropped:        $totalPacketsDropped")
        appendLine("║  Success Rate:   ${"%.1f".format(deliverySuccessRate * 100)}%")
        appendLine("║ ─── Routing ─────────────────────────────── ║")
        appendLine("║  Avg Hops:       ${"%.2f".format(averageHopCount)}")
        appendLine("║  Avg Latency:    ${"%.1f".format(averageDeliveryLatencyMs)}ms")
        appendLine("║  TTL Expirations:$totalTtlExpirations")
        appendLine("║  Loops Detected: $totalLoopsDetected")
        appendLine("║  Convergence:    ${if (routeConvergenceTimeMs >= 0) "${routeConvergenceTimeMs}ms" else "N/A"}")
        appendLine("║  Route Cache HR: ${"%.1f".format(routeCacheHitRatio * 100)}%")
        appendLine("║ ─── Dedup & Cache ───────────────────────── ║")
        appendLine("║  Dups Suppressed:$totalDuplicatesSuppressed")
        appendLine("║  Dedup Cache HR: ${"%.1f".format(duplicateCacheHitRatio * 100)}%")
        appendLine("║ ─── Store & Forward ─────────────────────── ║")
        appendLine("║  Stored:         $totalPacketsStored")
        appendLine("║  SF Delivered:   $totalPacketsDeliveredFromStore")
        appendLine("║ ─── Extended (Req 8) ─────────────────────── ║")
        appendLine("║  Retransmissions:$totalRetransmissions")
        appendLine("║  Congestion Evts:$totalCongestionEvents")
        appendLine("║  Avg Queue Wait: ${"%.1f".format(averageQueueWaitMs)}ms")
        appendLine("║  Avg Encrypt:    ${avgEncryptionLatencyNanos.toLong()}ns")
        appendLine("║  P99 Encrypt:    ${p99EncryptionLatencyNanos}ns")
        append    ("╚══════════════════════════════════════════════╝")
    }
}
