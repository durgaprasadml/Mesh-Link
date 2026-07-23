package com.meshlink.ai.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val congestionPredictor: CongestionPredictor,
    private val batteryPredictor: BatteryPredictor,
    private val transportPredictor: TransportPredictor
) {
    
    /**
     * Generates a list of plain-text recommendations based on the AI's current state.
     */
    fun generateRecommendations(
        currentQueueDepth: Int,
        recentBroadcastRate: Int,
        batteryPct: Int
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val congestionProb = congestionPredictor.predictCongestionProbability(currentQueueDepth, recentBroadcastRate)
        if (congestionProb > 0.7f) {
            recommendations.add("Delay large file transfers to clear severe network congestion.")
        }
        
        if (batteryPct < 20) {
            recommendations.add("Critical battery: Switch primarily to BLE transport for text-only messaging.")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Network is healthy. No optimizations needed.")
        }
        
        return recommendations
    }
}
