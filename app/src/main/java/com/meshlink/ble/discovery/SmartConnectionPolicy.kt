package com.meshlink.ble.discovery

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

enum class RetryState {
    ACTIVE,
    COOLDOWN,
    SUSPENDED
}

enum class FailureType {
    TIMEOUT,
    GATT_FAILURE,
    AUTHENTICATION_FAILURE,
    DEVICE_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    USER_DISCONNECTED
}

/**
 * Manages exponential backoff and connection decisions for peers.
 * Implements a configurable reconnect state machine.
 */
class SmartConnectionPolicy {
    
    companion object {
        private const val BASE_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 60000L
        
        // Configurable maximum retries before suspending a peer
        // Default is 10 which corresponds to roughly several minutes of total retry time
        var maxRetries: Int = 10
    }

    private val backoffMap = ConcurrentHashMap<String, Long>()
    private val nextAttemptTimeMap = ConcurrentHashMap<String, Long>()
    private val retryCountMap = ConcurrentHashMap<String, Int>()
    private val stateMap = ConcurrentHashMap<String, RetryState>()

    /**
     * Records a failed connection attempt and exponentially increases the backoff.
     */
    fun recordFailure(macAddress: String, failureType: FailureType = FailureType.GATT_FAILURE) {
        val currentRetries = retryCountMap.getOrDefault(macAddress, 0)
        
        when (failureType) {
            FailureType.USER_DISCONNECTED, FailureType.BLUETOOTH_DISABLED -> {
                // Immediate suspension - user intent or hardware off
                suspendPeer(macAddress)
                return
            }
            FailureType.AUTHENTICATION_FAILURE -> {
                // Heavier penalty for auth failure
                retryCountMap[macAddress] = currentRetries + 3
            }
            else -> {
                retryCountMap[macAddress] = currentRetries + 1
            }
        }
        
        val newRetries = retryCountMap[macAddress] ?: 0
        if (newRetries >= maxRetries) {
            suspendPeer(macAddress)
            return
        }

        val currentBackoff = backoffMap[macAddress] ?: (BASE_BACKOFF_MS / 2)
        
        // Double the backoff, capped at MAX_BACKOFF_MS
        val nextBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
        
        // Add +/- 20% jitter to prevent thundering herd reconnection storms
        val jitterMagnitude = (nextBackoff * 0.2).toLong()
        // Random.nextLong(min, max) where max is exclusive
        val jitter = if (jitterMagnitude > 0) Random.nextLong(-jitterMagnitude, jitterMagnitude + 1) else 0L
        
        val finalBackoff = (nextBackoff + jitter).coerceIn(BASE_BACKOFF_MS, MAX_BACKOFF_MS)
        
        backoffMap[macAddress] = finalBackoff
        nextAttemptTimeMap[macAddress] = System.currentTimeMillis() + finalBackoff
        stateMap[macAddress] = RetryState.COOLDOWN
    }

    private fun suspendPeer(macAddress: String) {
        stateMap[macAddress] = RetryState.SUSPENDED
        nextAttemptTimeMap.remove(macAddress) // Suspended means no automatic retry until reset
    }

    /**
     * Records a successful connection, resetting the backoff and retry counters.
     */
    fun recordSuccess(macAddress: String) {
        backoffMap.remove(macAddress)
        nextAttemptTimeMap.remove(macAddress)
        retryCountMap.remove(macAddress)
        stateMap[macAddress] = RetryState.ACTIVE
    }

    /**
     * Resets a peer's state so it can be retried immediately.
     */
    fun resetPeer(macAddress: String) {
        recordSuccess(macAddress)
    }

    /**
     * Determines if we are allowed to attempt a connection to this peer.
     * Takes into account the peer's backoff timer and suspension state.
     */
    fun canConnect(macAddress: String, isAlreadyConnected: Boolean): Boolean {
        if (isAlreadyConnected) return false
        
        val state = stateMap[macAddress] ?: RetryState.ACTIVE
        if (state == RetryState.SUSPENDED) return false
        
        val nextAttemptTime = nextAttemptTimeMap[macAddress] ?: 0L
        val can = System.currentTimeMillis() >= nextAttemptTime
        
        if (can && state == RetryState.COOLDOWN) {
            stateMap[macAddress] = RetryState.ACTIVE
        }
        return can
    }

    /**
     * Determines if we are allowed to attempt a connection to this peer.
     * Takes into account the peer's score and its backoff timer.
     */
    fun canConnect(record: PeerDiscoveryRecord, isAlreadyConnected: Boolean): Boolean {
        if (!canConnect(record.macAddress, isAlreadyConnected)) return false
        
        // Don't connect to garbage peers
        if (record.score < 20) return false
        
        return true
    }
    
    fun getBackoffTimeRemaining(macAddress: String): Long {
        if (stateMap[macAddress] == RetryState.SUSPENDED) return Long.MAX_VALUE
        val nextAttemptTime = nextAttemptTimeMap[macAddress] ?: 0L
        return (nextAttemptTime - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun getRetryState(macAddress: String): RetryState {
        return stateMap[macAddress] ?: RetryState.ACTIVE
    }
}
