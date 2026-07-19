package com.meshlink.scalability.engine

import javax.inject.Inject
import javax.inject.Singleton

data class Route(
    val peerIdHash: String,
    val hops: Int,
    val latencyMs: Long,
    val stabilityScore: Float
)

@Singleton
class RoutingOptimizationManager @Inject constructor() {

    fun prioritizeRoutes(availableRoutes: List<Route>): List<Route> {
        // Legacy behavior simply sorted by hops.
        // Scalable behavior prioritizes a hybrid score: Stability > Latency > Hops.
        
        return availableRoutes.sortedByDescending { route ->
            calculateEfficiencyScore(route)
        }
    }

    private fun calculateEfficiencyScore(route: Route): Float {
        val hopPenalty = route.hops * 0.1f
        val latencyPenalty = if (route.latencyMs > 500) 0.2f else 0.0f
        return (route.stabilityScore - hopPenalty - latencyPenalty).coerceAtLeast(0f)
    }
}
