package com.meshlink.ble.discovery

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Manages exponential backoff and connection decisions for peers.
 */
class SmartConnectionPolicy {
    
    companion object {
        private const val BASE_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30000L
    }

    private val backoffMap = ConcurrentHashMap<String, Long>()
    private val nextAttemptTimeMap = ConcurrentHashMap<String, Long>()

    /**
     * Records a failed connection attempt and exponentially increases the backoff.
     */
    fun recordFailure(macAddress: String) {
        val currentBackoff = backoffMap[macAddress] ?: BASE_BACKOFF_MS
        
        // Double the backoff, capped at MAX_BACKOFF_MS
        val nextBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
        
        // Add up to 20% jitter to prevent thundering herd reconnection storms
        val jitter = (nextBackoff * 0.2 * Random.nextDouble()).toLong()
        val finalBackoff = (nextBackoff + jitter).coerceAtMost(MAX_BACKOFF_MS)
        
        backoffMap[macAddress] = finalBackoff
        nextAttemptTimeMap[macAddress] = System.currentTimeMillis() + finalBackoff
    }

    /**
     * Records a successful connection, resetting the backoff.
     */
    fun recordSuccess(macAddress: String) {
        backoffMap.remove(macAddress)
        nextAttemptTimeMap.remove(macAddress)
    }

    /**
     * Determines if we are allowed to attempt a connection to this peer.
     * Takes into account the peer's score and its backoff timer.
     */
    fun canConnect(record: PeerDiscoveryRecord, isAlreadyConnected: Boolean): Boolean {
        if (isAlreadyConnected) return false
        
        // Don't connect to garbage peers
        if (record.score < 20) return false
        
        val nextAttemptTime = nextAttemptTimeMap[record.macAddress] ?: 0L
        return System.currentTimeMillis() >= nextAttemptTime
    }
    
    fun getBackoffTimeRemaining(macAddress: String): Long {
        val nextAttemptTime = nextAttemptTimeMap[macAddress] ?: 0L
        return (nextAttemptTime - System.currentTimeMillis()).coerceAtLeast(0L)
    }
}
