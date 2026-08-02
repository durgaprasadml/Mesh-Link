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

    // Priority: Hop count > Stability > RSSI > Latency > Transport
    private val hopCountWeight = 0.35f
    private val stabilityWeight = 0.25f
    private val linkQualityWeight = 0.20f
    private val latencyWeight = 0.10f
    private val reliabilityWeight = 0.10f

    fun calculateScore(entry: RouteEntry, packetType: com.meshlink.domain.model.PacketType? = null): Int {
        val m = entry.metrics

        // 1. Hop Count Penalty (0-100). 1 hop = 100, 10 hops = 10.
        val hopPenalty = min(100f, (entry.hops * (100f / 10f)))
        val hopScore = (100f - hopPenalty) * hopCountWeight

        // 2. Route Stability (0-100)
        val stabilityScore = (m.routeStability * 100f) * stabilityWeight

        // 3. Link Quality / RSSI (0-100)
        val rssiNormalized = max(0, min(100, ((m.rssi + 100) * 100) / 60))
        val linkQualityScore = rssiNormalized * linkQualityWeight

        // 4. Latency (0-100)
        val latencyPenalty = min(100f, (m.averageLatencyMs / 5f))
        val latencyScore = (100f - latencyPenalty) * latencyWeight

        // 5. Reliability (0-100)
        val reliability = if ((m.successfulDeliveries + m.failedDeliveries) > 0) {
            (m.historicalSuccessRate * 40f) + ((1.0f - m.packetLossRate) * 60f)
        } else {
            80f
        }
        val reliabilityScore = reliability * reliabilityWeight

        // 6. Battery & Congestion penalties
        var penalty = 0
        if (m.batteryLevel in 0..20) penalty += 15
        if (m.congestionLevel > 75) penalty += 20

        // 7. Transport Boost based on packet type
        val transportBoost = when (packetType) {
            com.meshlink.domain.model.PacketType.MEDIA_CHUNK,
            com.meshlink.domain.model.PacketType.MEDIA_META,
            com.meshlink.domain.model.PacketType.VOICE_FRAME,
            com.meshlink.domain.model.PacketType.VIDEO_FRAME,
            com.meshlink.domain.model.PacketType.RESOURCE_SYNC -> {
                when (entry.currentTransport) {
                    RouteType.WIFI_DIRECT -> 15
                    RouteType.HYBRID -> 12
                    RouteType.BLE -> 0
                }
            }
            com.meshlink.domain.model.PacketType.TEXT,
            com.meshlink.domain.model.PacketType.LOCATION,
            com.meshlink.domain.model.PacketType.SOS,
            com.meshlink.domain.model.PacketType.DELIVERY_ACK -> {
                when (entry.currentTransport) {
                    RouteType.BLE -> 8
                    RouteType.HYBRID -> 10
                    RouteType.WIFI_DIRECT -> 5
                }
            }
            else -> {
                when (entry.currentTransport) {
                    RouteType.WIFI_DIRECT -> 10
                    RouteType.HYBRID -> 8
                    RouteType.BLE -> 5
                }
            }
        }

        val baseScore = (hopScore + stabilityScore + linkQualityScore + latencyScore + reliabilityScore).toInt()
        val totalScore = baseScore - penalty + transportBoost

        return max(0, min(100, totalScore))
    }
    
    fun updateScores(routes: List<RouteEntry>, packetType: com.meshlink.domain.model.PacketType? = null) {
        routes.forEach {
            it.score = calculateScore(it, packetType)
        }
    }
}
