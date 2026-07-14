package com.meshlink.routing.engine

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class RouteScorer @Inject constructor() {

    // Advanced Phase E7 Configurable weights
    var linkQualityWeight = 0.25f
    var reliabilityWeight = 0.20f
    var batteryWeight = 0.15f
    var congestionWeight = 0.15f
    var latencyWeight = 0.10f
    var stabilityWeight = 0.05f
    var trustWeight = 0.05f
    var hopCountWeight = 0.05f

    fun calculateScore(entry: RouteEntry): Int {
        val m = entry.metrics

        // 1. Link Quality (0-100)
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

        // 5. Latency (0-100)
        val latencyPenalty = min(100f, (m.averageLatencyMs / 5f)) // e.g. 500ms = 100 penalty
        val latencyScore = (100f - latencyPenalty) * latencyWeight

        // 6. Stability (0-100)
        val stabilityScore = (m.routeStability * 100f) * stabilityWeight

        // 7. Trust (0-100)
        val trustScore = m.trustScore * trustWeight

        // 8. Hop Count (0-100)
        val hopPenalty = min(100f, (entry.hops * (100f / 15f)))
        val hopScore = (100f - hopPenalty) * hopCountWeight

        // Base total
        var totalScore = (linkQualityScore + reliabilityScore + batteryScore + congestionScore + 
                          latencyScore + stabilityScore + trustScore + hopScore).toInt()
        
        // Transport Type Boost
        // Wi-Fi Direct is given a slight boost due to bandwidth capability,
        // Hybrid gets a boost for redundancy.
        when (entry.routeType) {
            RouteType.WIFI_DIRECT -> totalScore += 10
            RouteType.HYBRID -> totalScore += 5
            RouteType.BLE -> { /* Base */ }
        }

        return max(0, min(100, totalScore))
    }
    
    fun updateScores(routes: List<RouteEntry>) {
        routes.forEach {
            it.score = calculateScore(it)
        }
    }
}
