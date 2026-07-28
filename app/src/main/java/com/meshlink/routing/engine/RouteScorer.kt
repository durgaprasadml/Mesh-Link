package com.meshlink.routing.engine

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.model.RouteEntry

/**
 * RouteScorer evaluates paths based on a multi-factor weighted formula.
 *
 * Scoring Formula:
 * Score = (LinkQuality * W1) + (Reliability * W2) + (Battery * W3) +
 *         (Congestion * W4) + (Latency * W5) + (Stability * W6) +
 *         (Trust * W7) + (HopCount * W8) + TransportBoost
 *
 * Weights (Sum to 1.0):
 * - W1 (LinkQuality): 0.25  (Smoothed RSSI)
 * - W2 (Reliability): 0.20  (Historical + Recent delivery success)
 * - W3 (Battery):     0.15  (Penalizes nodes with low battery)
 * - W4 (Congestion):  0.15  (Penalizes congested nodes)
 * - W5 (Latency):     0.10  (Inversely proportional to average latency)
 * - W6 (Stability):   0.05  (Uptime and connection stability)
 * - W7 (Trust):       0.05  (Node trust level)
 * - W8 (HopCount):    0.05  (Penalizes longer paths)
 */
@Singleton
class RouteScorer @Inject constructor() {

    // Advanced Phase E7 Configurable weights
    private val linkQualityWeight = 0.25f
    private val reliabilityWeight = 0.20f
    private val batteryWeight = 0.15f
    private val congestionWeight = 0.15f
    private val latencyWeight = 0.10f
    private val stabilityWeight = 0.05f
    private val trustWeight = 0.05f
    private val hopCountWeight = 0.05f

    fun calculateScore(entry: RouteEntry): Int {
        val m = entry.metrics

        // 1. Link Quality (0-100). Normalize RSSI (-100 to -40).
        val rssiNormalized = max(0, min(100, ((m.rssi + 100) * 100) / 60))
        val linkQualityScore = rssiNormalized * linkQualityWeight

        // 2. Reliability (0-100) combining Historical Success and Recent Packet Loss
        val reliability = if ((m.successfulDeliveries + m.failedDeliveries) > 0) {
            val hist = m.historicalSuccessRate * 100f
            val recent = (1.0f - m.packetLossRate) * 100f
            (hist * 0.4f) + (recent * 0.6f)
        } else {
            80f // New routes get benefit of the doubt
        }
        val reliabilityScore = reliability * reliabilityWeight

        // 3. Battery (0-100)
        val batteryScore = if (m.batteryLevel in 0..100) {
            // Non-linear penalty: battery < 15% drops score drastically
            if (m.batteryLevel < 15) {
                m.batteryLevel * 0.1f * batteryWeight 
            } else {
                m.batteryLevel * batteryWeight
            }
        } else {
            50f * batteryWeight // Unknown
        }

        // 4. Congestion (0-100)
        val congestionPenalty = max(0f, min(100f, m.congestionLevel.toFloat()))
        val congestionScore = (100f - congestionPenalty) * congestionWeight

        // 5. Latency (0-100). 500ms = 100 penalty
        val latencyPenalty = min(100f, (m.averageLatencyMs / 5f))
        val latencyScore = (100f - latencyPenalty) * latencyWeight

        // 6. Stability (0-100)
        val stabilityScore = (m.routeStability * 100f) * stabilityWeight

        // 7. Trust (0-100)
        val trustScore = m.trustScore * trustWeight

        // 8. Hop Count (0-100). 15 hops = max penalty
        val hopPenalty = min(100f, (entry.hops * (100f / 15f)))
        val hopScore = (100f - hopPenalty) * hopCountWeight

        // Base total
        var totalScore = (linkQualityScore + reliabilityScore + batteryScore + congestionScore + 
                          latencyScore + stabilityScore + trustScore + hopScore).toInt()

        return max(0, min(100, totalScore))
    }
    
    fun updateScores(routes: List<RouteEntry>) {
        routes.forEach {
            it.score = calculateScore(it)
        }
    }
}
