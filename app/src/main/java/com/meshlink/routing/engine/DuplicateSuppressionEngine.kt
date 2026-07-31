package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateSuppressionEngine @Inject constructor() {

    private val cache = ConcurrentHashMap<String, Long>()
    private val maxCacheSize = 5000
    private val ttlMs = 60_000L // 60-second duplicate suppression window

    /**
     * Builds a composite duplicate key using Packet ID, Sender ID, Sequence Number, and Target ID.
     */
    fun buildCompositeKey(packet: MeshPacket): String {
        return if (packet.sequenceNumber > 0L) {
            "${packet.senderId}_${packet.sequenceNumber}"
        } else {
            "${packet.senderId}_${packet.packetId}"
        }
    }

    /**
     * Checks if a packet is a duplicate and records it if new.
     * Returns true if it is a duplicate, false if new.
     */
    fun checkAndMark(packet: MeshPacket): Boolean {
        val key = buildCompositeKey(packet)
        val now = System.currentTimeMillis()

        // Clean up stale entries if size exceeds capacity
        if (cache.size > maxCacheSize) {
            evictStale(now)
            if (cache.size > maxCacheSize) {
                cache.clear() // Emergency fallback
            }
        }

        val existing = cache.putIfAbsent(key, now)
        if (existing != null) {
            if (now - existing > ttlMs) {
                // Expired key, refresh timestamp and accept
                cache[key] = now
                return false
            }
            return true // Duplicate detected!
        }
        return false // New packet
    }

    fun isDuplicate(packet: MeshPacket): Boolean {
        val key = buildCompositeKey(packet)
        val timestamp = cache[key] ?: return false
        val now = System.currentTimeMillis()
        if (now - timestamp > ttlMs) {
            cache.remove(key)
            return false
        }
        return true
    }

    private fun evictStale(now: Long) {
        cache.entries.removeIf { now - it.value > ttlMs }
    }

    fun size(): Int = cache.size

    fun clear() {
        cache.clear()
    }
}
