package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.config.RuntimeConfigManager
import javax.inject.Inject
import javax.inject.Singleton
import com.meshlink.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class RouteHealthMonitor @Inject constructor(
    private val routeCache: RouteCache,
    private val routeScorer: RouteScorer,
    private val configManager: RuntimeConfigManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "RouteHealthMonitor"
    }
    
    private var cleanupJob: Job? = null

    fun start() {
        if (cleanupJob?.isActive == true) return
        
        cleanupJob = applicationScope.launch {
            while (isActive) {
                delay(60_000L) // Check every minute
                try {
                    val staleThresholdMs = configManager.currentConfig.value.routeTimeoutMs
                    val evicted = routeCache.evictStaleRoutes(staleThresholdMs)
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
