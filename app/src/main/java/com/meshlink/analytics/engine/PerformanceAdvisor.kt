package com.meshlink.analytics.engine

import javax.inject.Inject
import javax.inject.Singleton

enum class RecommendationSeverity {
    INFO, WARNING, CRITICAL
}

data class OptimizationRecommendation(
    val issue: String,
    val recommendation: String,
    val severity: RecommendationSeverity
)

@Singleton
class PerformanceAdvisor @Inject constructor(
    private val meshAnalytics: MeshAnalyticsManager,
    private val routingAnalytics: RoutingAnalytics,
    private val transportAnalytics: TransportAnalytics
) {

    fun evaluateNetwork(): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()

        val currentAnalytics = meshAnalytics.analytics.value
        val routing = routingAnalytics.getRoutingScore()
        
        // 1. Packet Storm / Broadcast Flooding Detection
        if (routing.duplicateSuppressionRate > 0.6f) {
            recommendations.add(
                OptimizationRecommendation(
                    issue = "High Duplicate Packet Rate (${(routing.duplicateSuppressionRate * 100).toInt()}%)",
                    recommendation = "Reduce broadcast frequency or enable strict duplicate filtering.",
                    severity = RecommendationSeverity.WARNING
                )
            )
        }

        // 2. High Latency Detection
        if (currentAnalytics.averageRttMs > 1500) {
            recommendations.add(
                OptimizationRecommendation(
                    issue = "Network Latency High (RTT > 1.5s)",
                    recommendation = "Switch heavily loaded peers from BLE to Wi-Fi Direct if available.",
                    severity = RecommendationSeverity.CRITICAL
                )
            )
        }

        // 3. Transport Degradation
        val wifi = transportAnalytics.getWifiHealth()
        if (wifi.connectionSuccessRate < 0.5f) {
            recommendations.add(
                OptimizationRecommendation(
                    issue = "Wi-Fi Direct Instability Detected",
                    recommendation = "Restart Wi-Fi P2P Service or fallback to BLE for discovery.",
                    severity = RecommendationSeverity.WARNING
                )
            )
        }

        return recommendations
    }
}
