package com.meshlink.scalability.engine

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BroadcastOptimizationManager @Inject constructor() {

    // Track recently seen packet signatures to prevent infinite routing loops
    private val recentlySeenPackets = ConcurrentHashMap<String, Long>()

    fun shouldRelayPacket(packetSignature: String, currentHops: Int, maxTtl: Int, peerDensity: Int): Boolean {
        // 1. TTL Enforcement
        if (currentHops >= maxTtl) return false

        // 2. Duplicate Detection
        val now = System.currentTimeMillis()
        if (recentlySeenPackets.containsKey(packetSignature)) {
            val lastSeen = recentlySeenPackets[packetSignature] ?: 0L
            if (now - lastSeen < 60_000) { // Remember packets for 60 seconds
                return false
            }
        }
        
        // 3. Adaptive Suppression for Dense Environments
        // If we are in a dense 100-node cluster, we can rely on other nodes to flood.
        // We randomly drop relays if density > 50 to prevent packet amplification storms.
        if (peerDensity > 50 && Math.random() < 0.3) {
            return false // 30% chance to drop relaying obligation in ultra-dense areas
        }

        recentlySeenPackets[packetSignature] = now
        return true
    }
    
    fun clearCache() {
        recentlySeenPackets.clear()
    }
}
