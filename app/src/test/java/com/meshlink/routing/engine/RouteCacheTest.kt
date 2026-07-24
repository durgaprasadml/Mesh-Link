package com.meshlink.routing.engine

import com.meshlink.config.RuntimeConfigManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RouteCacheTest {

    private lateinit var configManager: RuntimeConfigManager
    private lateinit var routeCache: RouteCache

    @Before
    fun setup() {
        configManager = RuntimeConfigManager()
        routeCache = RouteCache(configManager)
    }

    @Test
    fun testRouteStability_SwitchingThreshold() {
        val dest = "node_C"
        
        // Initial primary route via A (Score 80)
        val routeA = RouteEntry(
            destinationId = dest,
            nextHop = "node_A",
            routeType = RouteType.BLE
        )
        routeA.score = 80
        routeCache.addOrUpdateRoute(routeA)

        assertEquals("node_A", routeCache.getRoutesForDestination(dest).first().nextHop)

        // New route via B comes in, slightly better (Score 85). Switching threshold is 10.
        // It should NOT replace node_A as primary to prevent flapping.
        val routeB = RouteEntry(
            destinationId = dest,
            nextHop = "node_B",
            routeType = RouteType.BLE
        )
        routeB.score = 85
        routeCache.addOrUpdateRoute(routeB)

        // The first route should still be A because 85 - 80 < 10
        assertEquals("node_A", routeCache.getRoutesForDestination(dest).first().nextHop)

        // Now B gets significantly better (Score 95). 95 - 80 >= 10.
        routeB.score = 95
        routeCache.addOrUpdateRoute(routeB)

        // The primary route should switch to B
        assertEquals("node_B", routeCache.getRoutesForDestination(dest).first().nextHop)
    }

    @Test
    fun testRssiEmaSmoothing() {
        val dest = "node_C"
        
        val routeA = RouteEntry(
            destinationId = dest,
            nextHop = "node_A",
            routeType = RouteType.BLE
        )
        routeA.metrics.rssi = -60
        routeCache.addOrUpdateRoute(routeA)
        
        // Second packet comes in with RSSI -90. Alpha is 0.3
        // New RSSI = (0.7 * -60) + (0.3 * -90) = -42 + -27 = -69
        val updatedRoute = RouteEntry(
            destinationId = dest,
            nextHop = "node_A",
            routeType = RouteType.BLE
        )
        updatedRoute.metrics.rssi = -90
        routeCache.addOrUpdateRoute(updatedRoute)
        
        val cachedRoute = routeCache.getRoutesForDestination(dest).first()
        assertEquals(-69, cachedRoute.metrics.rssi)
    }
}
