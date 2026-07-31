package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.RouteEntry
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteOptimizer @Inject constructor(
    private val routeCache: RouteCache
) {
    companion object {
        private const val TAG = "RouteOptimizer"
    }

    private val roundRobinCounter = AtomicInteger(0)

    /**
     * Dynamically adjusts TTL based on the estimated size of the network.
     */
    fun calculateDynamicTtl(): Int {
        val uniqueDestinations = routeCache.routeCount.value
        return when {
            uniqueDestinations <= 5 -> 4
            uniqueDestinations in 6..20 -> 8
            uniqueDestinations in 21..50 -> 12
            uniqueDestinations in 51..150 -> 15
            uniqueDestinations in 151..500 -> 20
            else -> 25
        }
    }

    /**
     * Determines the optimal route. Filters out routes predicted to fail.
     */
    fun getOptimalRoute(destinationId: String, excludeHops: Set<String> = emptySet()): RouteEntry? {
        val routes = routeCache.getRoutesForDestination(destinationId)
        if (routes.isEmpty()) return null

        val viableRoutes = routes.filter { it.nextHop !in excludeHops && !isPredictedToFail(it) }

        return if (viableRoutes.isNotEmpty()) {
            viableRoutes.maxByOrNull { it.score }
        } else {
            routes.filter { it.nextHop !in excludeHops }.maxByOrNull { it.score }
        }
    }

    /**
     * Multi-Path Load Balancing Strategy:
     * If multiple healthy routes exist with scores within a 15% delta of the top score,
     * distributes traffic across them using weighted round-robin to prevent overloading relay nodes.
     */
    fun getLoadBalancedRoute(destinationId: String, excludeHops: Set<String> = emptySet()): RouteEntry? {
        val routes = routeCache.getRoutesForDestination(destinationId)
            .filter { it.nextHop !in excludeHops && !isPredictedToFail(it) && it.score >= 30 }

        if (routes.isEmpty()) return getOptimalRoute(destinationId, excludeHops)
        if (routes.size == 1) return routes.first()

        val maxScore = routes.maxOf { it.score }
        // Candidates must be within 15 points of top score
        val topCandidates = routes.filter { it.score >= maxScore - 15 }

        if (topCandidates.size <= 1) return topCandidates.firstOrNull() ?: routes.maxByOrNull { it.score }

        // Round-robin selection among top healthy candidates
        val index = Math.abs(roundRobinCounter.getAndIncrement()) % topCandidates.size
        val selected = topCandidates[index]
        MeshLogger.d(TAG, "Load balancer selected path via ${selected.nextHop} (Score: ${selected.score}) out of ${topCandidates.size} candidates")
        return selected
    }

    /**
     * Returns secondary routes for failover / multipath if primary fails.
     */
    fun getBackupRoutes(destinationId: String, primaryNextHop: String): List<RouteEntry> {
        val routes = routeCache.getRoutesForDestination(destinationId)
        return routes.filter { it.nextHop != primaryNextHop && !isPredictedToFail(it) }
            .sortedByDescending { it.score }
    }

    /**
     * Predictive Failure Analysis
     */
    private fun isPredictedToFail(route: RouteEntry): Boolean {
        val m = route.metrics
        if (m.batteryLevel in 0..10) return true
        if (m.congestionLevel > 90) return true
        if (m.packetLossRate > 0.8f) return true
        return false
    }
}
