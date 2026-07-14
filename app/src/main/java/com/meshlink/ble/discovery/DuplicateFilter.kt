package com.meshlink.ble.discovery

import java.util.concurrent.ConcurrentHashMap

/**
 * Suppresses duplicate advertisements from the same device within a specific time window
 * to reduce CPU and logging overhead.
 */
class DuplicateFilter(private val windowMillis: Long = 2000L) {
    private val lastSeenMap = ConcurrentHashMap<String, Long>()

    /**
     * Checks if the device should be processed or dropped as a duplicate.
     * @return true if it's a new/valid advertisement, false if it's a duplicate.
     */
    fun shouldProcess(macAddress: String): Boolean {
        val now = System.currentTimeMillis()
        val lastSeen = lastSeenMap[macAddress]
        
        if (lastSeen == null || (now - lastSeen) > windowMillis) {
            lastSeenMap[macAddress] = now
            return true
        }
        
        return false
    }

    /**
     * Cleans up stale entries to prevent memory leaks in the ConcurrentHashMap.
     */
    fun prune() {
        val now = System.currentTimeMillis()
        val iterator = lastSeenMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > windowMillis * 2) {
                iterator.remove()
            }
        }
    }
    
    fun clear() {
        lastSeenMap.clear()
    }
}
