package com.meshlink.routing.engine

import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Thread-safe routing table managing network routes across the mesh topology.
 */
@Singleton
class RoutingTable @Inject constructor() {

    // Key: Destination ID -> List of possible routes (multipath support)
    private val table = ConcurrentHashMap<String, MutableList<RouteEntry>>()
    private val lock = ReentrantLock()

    /**
     * Adds or updates a route entry in the routing table.
     */
    fun addOrUpdateRoute(entry: RouteEntry) {
        lock.withLock {
            val destRoutes = table.getOrPut(entry.destinationId) { mutableListOf() }
            val existing = destRoutes.find { it.nextHop == entry.nextHop }

            if (existing != null) {
                existing.lastSeen = entry.lastSeen
                existing.hops = minOf(existing.hops, entry.hops)
                existing.expirationTime = entry.expirationTime
                existing.score = entry.score
                existing.metrics.updateRssi(entry.metrics.rssi, 0.3f)
                existing.metrics.routeStability = entry.metrics.routeStability
            } else {
                destRoutes.add(entry)
            }

            // Sort routes by score descending (highest quality path first)
            destRoutes.sortByDescending { it.score }
        }
    }

    /**
     * Finds the optimal next hop for a destination.
     */
    fun getOptimalRoute(destinationId: String, excludeHops: Set<String> = emptySet()): RouteEntry? {
        return lock.withLock {
            table[destinationId]?.find { it.nextHop !in excludeHops && System.currentTimeMillis() < it.expirationTime }
        }
    }

    /**
     * Returns all multipath backup routes for a destination.
     */
    fun getBackupRoutes(destinationId: String, primaryNextHop: String): List<RouteEntry> {
        return lock.withLock {
            table[destinationId]?.filter { it.nextHop != primaryNextHop && System.currentTimeMillis() < it.expirationTime } ?: emptyList()
        }
    }

    /**
     * Invalidates and removes all routes passing through a specific next hop (e.g. peer disconnected).
     */
    fun invalidateRoutesViaHop(nextHop: String): Int {
        var removedCount = 0
        lock.withLock {
            table.values.forEach { routes ->
                if (routes.removeIf { it.nextHop == nextHop }) {
                    removedCount++
                }
            }
            table.entries.removeIf { it.value.isEmpty() }
        }
        return removedCount
    }

    /**
     * Evicts expired routes from the table.
     */
    fun evictExpiredRoutes(): Int {
        val now = System.currentTimeMillis()
        var evictedCount = 0
        lock.withLock {
            table.entries.removeIf { (_, routes) ->
                val before = routes.size
                routes.removeIf { now > it.expirationTime }
                evictedCount += (before - routes.size)
                routes.isEmpty()
            }
        }
        return evictedCount
    }

    /**
     * Returns a snapshot of all active routing entries.
     */
    fun getAllRoutes(): Map<String, List<RouteEntry>> {
        return lock.withLock {
            table.mapValues { it.value.toList() }
        }
    }

    /**
     * Returns total number of active destinations in routing table.
     */
    fun getDestinationCount(): Int {
        return table.size
    }

    fun clear() {
        lock.withLock {
            table.clear()
        }
    }
}
