package com.meshlink.routing.engine


import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class RouteOptimizer @Inject constructor(
    private val routeCache: RouteCache
) {
    companion object {
        private const val TAG = "RouteOptimizer"
    }
    
    /**
     * Dynamically adjusts TTL based on the estimated size of the network.
     * Large meshes need higher TTL, but not so high it causes broadcast storms.
     */
    fun calculateDynamicTtl(): Int {
        val uniqueDestinations = routeCache.routeCount.value
        return when {
            uniqueDestinations <= 5 -> 4
            uniqueDestinations in 6..20 -> 8
            uniqueDestinations in 21..50 -> 12
            uniqueDestinations in 51..150 -> 15
            uniqueDestinations in 151..500 -> 20
            else -> 25 // Max TTL for very large networks
        }
    }

    /**
     * Determines the optimal route. It filters out routes that are predicted to fail.
     */
    fun getOptimalRoute(destinationId: String, excludeHops: Set<String> = emptySet()): RouteEntry? {
        val routes = routeCache.getRoutesForDestination(destinationId)
        if (routes.isEmpty()) return null
        
        // Predictive Route Failure:
        val viableRoutes = routes.filter { it.nextHop !in excludeHops && !isPredictedToFail(it) }
        
        return if (viableRoutes.isNotEmpty()) {
            viableRoutes.maxByOrNull { it.score }
        } else {
            // Fallback to any route if all are predicted to fail (desperation mode)
            routes.filter { it.nextHop !in excludeHops }.maxByOrNull { it.score }
        }
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
        
        // Hard thresholds as fallbacks
        if (m.batteryLevel in 0..10) return true
        if (m.congestionLevel > 90) return true
        
        return false
    }
}
