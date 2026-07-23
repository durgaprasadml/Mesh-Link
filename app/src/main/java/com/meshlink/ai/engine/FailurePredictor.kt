package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import com.meshlink.routing.engine.RouteEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailurePredictor @Inject constructor(
    private val learningRepository: LearningRepository
) {
    /**
     * Predicts whether a route will fail in the immediate future (e.g. next 5-10 seconds)
     * by looking at the combination of stability and connection drops.
     */
    fun willRouteFailSoon(route: RouteEntry): Boolean {
        val metrics = learningRepository.getMetricsForPeer(route.nextHop)
        
        // If a node drops connection frequently and stability is currently low, a failure is imminent.
        if (metrics.connectionDrops > 10 && route.metrics.routeStability < 0.3f) {
            return true
        }
        
        // If they have been offline for a while and just popped up, they might be highly volatile.
        val now = System.currentTimeMillis()
        val timeSinceLastSeen = now - metrics.lastSeenTimestamp
        if (metrics.totalPacketsSent > 100 && metrics.totalPacketsDelivered < 10) {
             return true
        }
        
        return false
    }
}
