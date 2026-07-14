package com.meshlink.routing.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteManager @Inject constructor(
    val routeCache: RouteCache,
    private val routeScorer: RouteScorer,
    private val routeOptimizer: RouteOptimizer
) {

    fun updateRoute(destinationId: String, nextHop: String, hops: Int, rssi: Int = -100, trustScore: Int = 50, type: RouteType = RouteType.BLE) {
        val entry = RouteEntry(
            destinationId = destinationId,
            nextHop = nextHop,
            hops = hops,
            routeType = type,
            lastSeen = System.currentTimeMillis()
        )
        entry.metrics.rssi = rssi
        entry.metrics.trustScore = trustScore
        entry.score = routeScorer.calculateScore(entry)
        
        routeCache.addOrUpdateRoute(entry)
    }

    fun getOptimalRoute(destinationId: String, excludeHops: Set<String> = emptySet()): RouteEntry? {
        return routeOptimizer.getOptimalRoute(destinationId, excludeHops)
    }

    fun getBackupRoutes(destinationId: String, primaryNextHop: String): List<RouteEntry> {
        return routeOptimizer.getBackupRoutes(destinationId, primaryNextHop)
    }

    fun recordDeliverySuccess(destinationId: String, nextHop: String, latencyMs: Long) {
        routeCache.recordDeliverySuccess(destinationId, nextHop, latencyMs)
    }

    fun recordDeliveryFailure(destinationId: String, nextHop: String) {
        routeCache.recordDeliveryFailure(destinationId, nextHop)
    }

    fun handlePeerDisconnected(nextHop: String) {
        routeCache.removeRoutesViaHop(nextHop)
    }
}
