package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import com.meshlink.routing.engine.RouteEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutePredictionEngine @Inject constructor(
    private val learningRepository: LearningRepository
) {
    /**
     * Predicts the likelihood of a route succeeding (0.0 to 1.0) based on historical data
     * and current heuristics.
     */
    fun predictRouteSuccessProbability(route: RouteEntry): Float {
        val metrics = learningRepository.getMetricsForPeer(route.nextHop)
        
        val totalAttempts = metrics.totalPacketsSent
        if (totalAttempts < 5) {
            // Not enough data to predict, trust the base route metrics
            return if (route.metrics.routeStability > 0.5f) 0.8f else 0.5f
        }
        
        val historicalSuccessRate = metrics.totalPacketsDelivered.toFloat() / totalAttempts.toFloat()
        
        // Decay the historical success rate if there have been recent drops
        val recentDropPenalty = if (metrics.connectionDrops > 5) 0.2f else 0.0f
        
        var probability = historicalSuccessRate - recentDropPenalty
        
        // Cap between 0.1 and 0.99
        return probability.coerceIn(0.1f, 0.99f)
    }

    /**
     * Predicts expected latency based on historical moving averages for this peer.
     */
    fun predictLatencyMs(route: RouteEntry): Long {
        val metrics = learningRepository.getMetricsForPeer(route.nextHop)
        if (metrics.averageLatencyMs == 0L) {
            return (route.hops * 50L) // Guess 50ms per hop
        }
        return metrics.averageLatencyMs
    }
}
