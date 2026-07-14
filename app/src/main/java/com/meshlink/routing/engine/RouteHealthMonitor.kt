package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class RouteHealthMonitor @Inject constructor(
    private val routeCache: RouteCache,
    private val routeScorer: RouteScorer
) {
    companion object {
        private const val TAG = "RouteHealthMonitor"
        private const val ROUTE_STALE_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private var cleanupJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        if (cleanupJob?.isActive == true) return
        
        cleanupJob = scope.launch {
            while (isActive) {
                delay(60_000L) // Check every minute
                try {
                    val evicted = routeCache.evictStaleRoutes(ROUTE_STALE_MS)
                    if (evicted > 0) {
                        MeshLogger.d(TAG, "Evicted $evicted stale routes.")
                    }
                    
                    // Periodically rescore all routes in cache based on time decay
                    val allDests = routeCache.getAllDestinations()
                    allDests.forEach { dest ->
                        val routes = routeCache.getRoutesForDestination(dest)
                        // Apply time decay to metrics if needed, then rescore
                        routeScorer.updateScores(routes)
                    }
                    
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Health monitor error: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        cleanupJob?.cancel()
        cleanupJob = null
    }
}
