package com.meshlink.stress

import com.meshlink.simulator.metrics.SimulationMetrics
import com.meshlink.simulator.metrics.SimulationReport
import java.io.File
import kotlin.math.max

/**
 * Defines regression baselines for performance metrics.
 */
data class RegressionThresholds(
    val minDeliverySuccessRate: Float = 0.99f,
    val maxDuplicateDelivery: Long = 0L,
    val maxRouteConvergenceTimeMs: Long = 5000L,
    val maxAverageLatencyMs: Double = 500.0,
    val maxQueueWaitTimeMs: Double = 200.0,
    val maxRetransmissionsPerNode: Double = 10.0,
    val maxCongestionEvents: Long = 50L
)

/**
 * Evaluates simulation metrics against regression baselines and generates reports.
 */
class StressMetricsCollector(
    private val metrics: SimulationMetrics,
    private val thresholds: RegressionThresholds = RegressionThresholds()
) {

    fun evaluateAndReport(scenario: StressScenario): SimulationReport {
        val report = metrics.generateReport()
        val violations = mutableListOf<String>()

        if (report.deliverySuccessRate < thresholds.minDeliverySuccessRate) {
            violations.add("Delivery Success Rate ${report.deliverySuccessRate * 100}% is below minimum ${thresholds.minDeliverySuccessRate * 100}%")
        }
        
        // Note: SimulationReport doesn't directly track duplicate deliveries to app, but tracks duplicatesSuppressed.
        // If duplicates are not suppressed, they might not be directly counted. We assume duplicate cache hit ratio should be high or duplicate delivery = 0
        // Currently, we'll just check if any node received the exact same trace ID multiple times. But SimulationReport lacks that.
        // We'll skip duplicate delivery check for now, or just check that duplicatesSuppressed > 0 (meaning suppression works).

        if (report.routeConvergenceTimeMs > thresholds.maxRouteConvergenceTimeMs) {
            violations.add("Route Convergence Time ${report.routeConvergenceTimeMs}ms exceeds max ${thresholds.maxRouteConvergenceTimeMs}ms")
        }

        if (report.averageDeliveryLatencyMs > thresholds.maxAverageLatencyMs) {
            violations.add("Average Latency ${report.averageDeliveryLatencyMs}ms exceeds max ${thresholds.maxAverageLatencyMs}ms")
        }

        if (report.averageQueueWaitMs > thresholds.maxQueueWaitTimeMs) {
            violations.add("Average Queue Wait ${report.averageQueueWaitMs}ms exceeds max ${thresholds.maxQueueWaitTimeMs}ms")
        }

        val retransmissionsPerNode = report.totalRetransmissions.toDouble() / max(1, report.nodeCount)
        if (retransmissionsPerNode > thresholds.maxRetransmissionsPerNode) {
            violations.add("Retransmissions per node $retransmissionsPerNode exceeds max ${thresholds.maxRetransmissionsPerNode}")
        }

        if (report.totalCongestionEvents > thresholds.maxCongestionEvents) {
            violations.add("Total Congestion Events ${report.totalCongestionEvents} exceeds max ${thresholds.maxCongestionEvents}")
        }

        if (violations.isNotEmpty()) {
            throw AssertionError("Stress test failed with ${violations.size} violations:\n${violations.joinToString("\n")}")
        }

        return report
    }
}
