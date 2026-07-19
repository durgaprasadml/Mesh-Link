package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.common.logger.MeshLogger
import java.util.Collections
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
    private val routeOptimizer: RouteOptimizer
) {
    companion object {
        private const val TAG = "RoutingEngine"
        private const val DEDUP_CACHE_SIZE = 20000
    }
    
    // Tracks processed packets to prevent broadcast storms and routing loops
    private val processedPackets = Collections.newSetFromMap(
        Collections.synchronizedMap(
            object : java.util.LinkedHashMap<String, Boolean>(DEDUP_CACHE_SIZE, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                    return size > DEDUP_CACHE_SIZE
                }
            }
        )
    )

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
        return processedPackets.add(packetId)
    }

    /**
     * Determines if a packet is caught in a routing loop.
     */
    fun isRoutingLoop(packet: MeshPacket, localMeshId: String): Boolean {
        return localMeshId.isNotBlank() && packet.visitedPath.contains(localMeshId)
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
        if (packetType == com.meshlink.domain.model.PacketType.SOS) return true // Always relay SOS
        
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
