package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.common.logger.MeshLogger
import com.meshlink.config.RuntimeConfigManager
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class RoutingEngine @Inject constructor(
    val routeManager: RouteManager,
    val qosManager: QoSManager,
    val congestionMonitor: CongestionMonitor,
    val routeHealthMonitor: RouteHealthMonitor,
    val topologyEngine: NetworkTopologyEngine,
    val batteryAwareNetworking: BatteryAwareNetworking,
    val transportManager: IntelligentTransportManager,
    val retryEngine: IntelligentRetryEngine,
    val queueOptimizer: QueueOptimizer,
    private val routeOptimizer: RouteOptimizer,
    private val configManager: RuntimeConfigManager
) {
    companion object {
        private const val TAG = "RoutingEngine"
    }

    /**
     * A production-grade time-bounded, size-bounded duplicate cache.
     * Uses O(1) lookup and lazy eviction for efficiency.
     */
    private inner class TimeBoundedDuplicateCache {
        private val cache = ConcurrentHashMap<String, Long>()

        fun addIfAbsent(packetId: String): Boolean {
            val config = configManager.currentConfig.value
            val now = System.currentTimeMillis()
            
            // Lazy eviction: cleanup periodically if we exceed the threshold
            if (cache.size > config.duplicateCacheSize) {
                evictStale(now, config.duplicateCacheLifetimeMs)
                // Hard cap fallback
                if (cache.size > config.duplicateCacheSize) {
                    cache.clear() // Drastic, but ensures O(1) constraints if heavily flooded
                }
            }
            
            val existing = cache.putIfAbsent(packetId, now)
            if (existing != null) {
                // Was already in cache. Check if it's expired.
                if (now - existing > config.duplicateCacheLifetimeMs) {
                    // It was expired, so we should consider it new and update timestamp
                    cache[packetId] = now
                    return true
                }
                return false
            }
            return true
        }

        private fun evictStale(now: Long, maxAgeMs: Long) {
            cache.entries.removeIf { now - it.value > maxAgeMs }
        }
    }
    
    private val duplicateCache = TimeBoundedDuplicateCache()

    fun start() {
        routeHealthMonitor.start()
    }

    fun stop() {
        routeHealthMonitor.stop()
    }

    /**
     * Records a packet as processed. Returns true if it was new, false if duplicate.
     */
    fun markPacketProcessed(packetId: String): Boolean {
        return duplicateCache.addIfAbsent(packetId)
    }

    /**
     * Determines if a packet is caught in a routing loop.
     * Enforces strict validation based on path and ttl.
     */
    fun isRoutingLoop(packet: MeshPacket, localMeshId: String): Boolean {
        if (packet.ttl <= 0) return true
        if (localMeshId.isNotBlank() && packet.visitedPath.contains(localMeshId)) {
            MeshLogger.w(TAG, "Routing loop detected: Local ID $localMeshId already in visited path for packet ${packet.packetId}")
            return true
        }
        return false
    }
    
    /**
     * Calculates the dynamic TTL for a new outgoing packet.
     * Incorporates Battery awareness (reduce TTL if battery is low to prevent network abuse).
     */
    fun calculateInitialTtl(packetType: com.meshlink.domain.model.PacketType): Int {
        var baseTtl = routeOptimizer.calculateDynamicTtl()
        
        if (batteryAwareNetworking.powerState.value == PowerState.CRITICAL) {
            baseTtl = Math.max(1, baseTtl / 2) // Shrink blast radius if battery is dying
        }
        
        return qosManager.getMaxTtl(packetType, baseTtl)
    }

    /**
     * Probabilistic Relay for broadcasts.
     */
    fun shouldRelayBroadcast(packetType: com.meshlink.domain.model.PacketType): Boolean {
        if (packetType == com.meshlink.domain.model.PacketType.SOS ||
            packetType == com.meshlink.domain.model.PacketType.TEXT ||
            packetType == com.meshlink.domain.model.PacketType.LOCATION) {
            return true // Always relay critical user data
        }
        
        if (!batteryAwareNetworking.canRelayBackgroundTraffic()) {
            return false // Drop if battery is critical
        }
        
        val prob = batteryAwareNetworking.getBroadcastProbability()
        if (prob < 1.0f) {
            return Random.nextFloat() <= prob
        }
        return true
    }

    /**
     * Returns the optimal next hop for the packet, or null if broadcast is required.
     */
    fun getNextHopForForwarding(packet: MeshPacket, connectedNodes: Set<String>, excludeHop: String): String? {
        // SOS packets ignore directed forwarding and always broadcast to maximize delivery chances
        if (packet.type == com.meshlink.domain.model.PacketType.SOS) return null

        val optimalRoute = routeManager.getOptimalRoute(packet.targetId, setOf(excludeHop))
        if (optimalRoute != null && connectedNodes.contains(optimalRoute.nextHop)) {
            return optimalRoute.nextHop
        }
        
        return null
    }
}
