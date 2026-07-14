package com.meshlink.routing.engine

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Singleton
class RouteCache @Inject constructor() {
    
    // Key: destinationId -> Value: List of possible routes (multipath support)
    private val routes = ConcurrentHashMap<String, MutableList<RouteEntry>>()
    
    private val _routeCount = MutableStateFlow(0)
    val routeCount: StateFlow<Int> = _routeCount.asStateFlow()

    fun getRoutesForDestination(destinationId: String): List<RouteEntry> {
        return routes[destinationId]?.toList() ?: emptyList()
    }

    fun getAllDestinations(): List<String> {
        return routes.keys().toList()
    }

    private val lock = ReentrantLock()

    fun addOrUpdateRoute(entry: RouteEntry) {
        lock.withLock {
            val destRoutes = routes.getOrPut(entry.destinationId) { mutableListOf() }
            val existing = destRoutes.find { it.nextHop == entry.nextHop }
            if (existing != null) {
                // Update existing route metrics and freshness
                existing.lastSeen = entry.lastSeen
                if (entry.hops < existing.hops) existing.hops = entry.hops
                existing.metrics.rssi = entry.metrics.rssi
                existing.score = entry.score
            } else {
                destRoutes.add(entry)
            }
            // Keep paths sorted by score (highest first)
            destRoutes.sortByDescending { it.score }
        }
        
        _routeCount.update { routes.values.sumOf { it.size } }
    }

    fun recordDeliverySuccess(destinationId: String, nextHop: String, latencyMs: Long) {
        lock.withLock {
            routes[destinationId]?.let { list ->
                list.find { it.nextHop == nextHop }?.let { route ->
                    route.metrics.recordSuccess(latencyMs)
                    route.isVerified = true
                    route.lastSeen = System.currentTimeMillis()
                }
            }
        }
    }

    fun recordDeliveryFailure(destinationId: String, nextHop: String) {
        lock.withLock {
            routes[destinationId]?.let { list ->
                list.find { it.nextHop == nextHop }?.metrics?.recordFailure()
            }
        }
    }

    fun evictStaleRoutes(staleThresholdMs: Long): Int {
        val now = System.currentTimeMillis()
        var evictedCount = 0
        
        lock.withLock {
            routes.entries.removeIf { (_, destRoutes) ->
                val originalSize = destRoutes.size
                destRoutes.removeIf { now - it.lastSeen > staleThresholdMs }
                evictedCount += (originalSize - destRoutes.size)
                destRoutes.isEmpty()
            }
        }
        
        if (evictedCount > 0) {
            _routeCount.update { routes.values.sumOf { it.size } }
        }
        
        return evictedCount
    }
    
    fun removeRoutesViaHop(nextHop: String) {
        var removed = false
        lock.withLock {
            routes.values.forEach { list ->
                if (list.removeIf { it.nextHop == nextHop }) {
                    removed = true
                }
            }
            // Clean up empty destinations
            routes.entries.removeIf { it.value.isEmpty() }
        }
        
        if (removed) {
            _routeCount.update { routes.values.sumOf { it.size } }
        }
    }
    
    fun clear() {
        lock.withLock {
            routes.clear()
        }
        _routeCount.value = 0
    }
}
