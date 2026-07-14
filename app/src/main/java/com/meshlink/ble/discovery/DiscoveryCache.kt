package com.meshlink.ble.discovery

import java.util.concurrent.ConcurrentHashMap

data class PeerDiscoveryRecord(
    val macAddress: String,
    val meshId: String,
    var name: String,
    val rssiFilter: RssiFilter = RssiFilter(),
    var smoothedRssi: Int = -100,
    var lastSeenMillis: Long = System.currentTimeMillis(),
    var failedAttempts: Int = 0,
    var capabilities: Byte = 0, // Battery, Routing, etc.
    var score: Int = 0,
    var state: PeerLifecycleState = PeerLifecycleState.UNKNOWN
)

/**
 * In-memory thread-safe cache of all discovered peers.
 */
class DiscoveryCache {
    private val peers = ConcurrentHashMap<String, PeerDiscoveryRecord>()

    fun getOrPut(macAddress: String, meshId: String, defaultName: String): PeerDiscoveryRecord {
        return peers.getOrPut(macAddress) {
            PeerDiscoveryRecord(macAddress = macAddress, meshId = meshId, name = defaultName)
        }
    }

    fun get(macAddress: String): PeerDiscoveryRecord? {
        return peers[macAddress]
    }

    fun getAll(): List<PeerDiscoveryRecord> {
        return peers.values.toList()
    }

    /**
     * Removes peers that haven't been seen in the specified timeout.
     * @return Number of evicted peers.
     */
    fun evictStale(timeoutMillis: Long): Int {
        val now = System.currentTimeMillis()
        var count = 0
        val iterator = peers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val record = entry.value
            // Only evict if not connected/connecting
            if ((now - record.lastSeenMillis > timeoutMillis) && 
                record.state != PeerLifecycleState.CONNECTED &&
                record.state != PeerLifecycleState.CONNECTING &&
                record.state != PeerLifecycleState.READY) {
                
                iterator.remove()
                count++
            }
        }
        return count
    }
    
    fun clear() {
        peers.clear()
    }
}
