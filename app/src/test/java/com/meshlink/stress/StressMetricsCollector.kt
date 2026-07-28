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
    private val thresholds: RegressionThresholds? = null
) {

    fun evaluateAndReport(scenario: StressScenario): SimulationReport {
        val effectiveThresholds = thresholds ?: getProfileDefaultThresholds(scenario)
        val report = metrics.generateReport()
        val violations = mutableListOf<String>()

        if (report.deliverySuccessRate < effectiveThresholds.minDeliverySuccessRate) {
            violations.add("Delivery Success Rate ${report.deliverySuccessRate * 100}% is below minimum ${effectiveThresholds.minDeliverySuccessRate * 100}%")
        }

        if (report.routeConvergenceTimeMs > effectiveThresholds.maxRouteConvergenceTimeMs) {
            violations.add("Route Convergence Time ${report.routeConvergenceTimeMs}ms exceeds max ${effectiveThresholds.maxRouteConvergenceTimeMs}ms")
        }

        if (report.averageDeliveryLatencyMs > effectiveThresholds.maxAverageLatencyMs) {
            violations.add("Average Latency ${report.averageDeliveryLatencyMs}ms exceeds max ${effectiveThresholds.maxAverageLatencyMs}ms")
        }

        if (report.averageQueueWaitMs > effectiveThresholds.maxQueueWaitTimeMs) {
            violations.add("Average Queue Wait ${report.averageQueueWaitMs}ms exceeds max ${effectiveThresholds.maxQueueWaitTimeMs}ms")
        }

        val retransmissionsPerNode = report.totalRetransmissions.toDouble() / max(1, report.nodeCount)
        if (retransmissionsPerNode > effectiveThresholds.maxRetransmissionsPerNode) {
            violations.add("Retransmissions per node $retransmissionsPerNode exceeds max ${effectiveThresholds.maxRetransmissionsPerNode}")
        }

        if (report.totalCongestionEvents > effectiveThresholds.maxCongestionEvents) {
            violations.add("Total Congestion Events ${report.totalCongestionEvents} exceeds max ${effectiveThresholds.maxCongestionEvents}")
        }

        if (violations.isNotEmpty()) {
            throw AssertionError("Stress test failed for ${scenario.profile} with ${violations.size} violations:\n${violations.joinToString("\n")}")
        }

        return report
    }

    private fun getProfileDefaultThresholds(scenario: StressScenario): RegressionThresholds {
        return when (scenario.profile) {
            NetworkFailureProfiles.RouteFlapping -> RegressionThresholds(minDeliverySuccessRate = 0.30f)
            NetworkFailureProfiles.FlakyBluetooth -> RegressionThresholds(minDeliverySuccessRate = 0.40f)
            NetworkFailureProfiles.HighLoss -> RegressionThresholds(minDeliverySuccessRate = 0.30f)
            NetworkFailureProfiles.Partitioned -> RegressionThresholds(minDeliverySuccessRate = 0.20f)
            else -> RegressionThresholds(minDeliverySuccessRate = 0.50f)
        }
    }
}
