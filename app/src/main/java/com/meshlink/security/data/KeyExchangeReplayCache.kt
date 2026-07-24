package com.meshlink.security.data

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Collections
import java.util.LinkedHashMap

@Singleton
class KeyExchangeReplayCache @Inject constructor() {

    private val TAG = "KeyExchangeReplayCache"

    // Stores: "peerId:nonce" -> expiryTimeMs
    // Thread-safe and memory bounded to prevent exhaustion attacks.
    private val nonceCache: MutableMap<String, Long> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(
            SecurityConstants.KEY_EXCHANGE_REPLAY_CACHE_SIZE,
            0.75f,
            true // access order
        ) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > SecurityConstants.KEY_EXCHANGE_REPLAY_CACHE_SIZE
            }
        }
    )

    // Tracks last handshake time per peer for rate limiting
    private val handshakeRateLimits = ConcurrentHashMap<String, Long>()

    /**
     * Checks if the given nonce has already been processed for this peer.
     * Also performs lazy cleanup of expired nonces.
     */
    fun isReplay(peerId: String, nonce: String): Boolean {
        lazyCleanup()
        
        val key = "$peerId:$nonce"
        val expiryTime = nonceCache[key]
        val now = System.currentTimeMillis()

        if (expiryTime != null && now <= expiryTime) {
            MeshLogger.w(TAG, "Replay detected: Nonce already processed for peer $peerId")
            return true
        }
        
        return false
    }

    /**
     * Records a nonce as processed to prevent future replays.
     */
    fun recordNonce(peerId: String, nonce: String) {
        val key = "$peerId:$nonce"
        val expiryTime = System.currentTimeMillis() + SecurityConstants.KEY_EXCHANGE_WINDOW_MS
        nonceCache[key] = expiryTime
    }

    /**
     * Checks if the peer is allowed to perform a fresh handshake based on rate limits.
     * Rate limit is bypassed for explicit reconnects or key rotations (handled upstream).
     */
    fun canProcessHandshake(peerId: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = handshakeRateLimits[peerId] ?: 0L
        
        if (now - lastTime < SecurityConstants.HANDSHAKE_RATE_LIMIT_MS) {
            MeshLogger.w(TAG, "Handshake rate limited for peer $peerId")
            return false
        }
        
        return true
    }

    /**
     * Updates the last handshake time for a peer.
     */
    fun recordHandshake(peerId: String) {
        handshakeRateLimits[peerId] = System.currentTimeMillis()
    }

    /**
     * Explicitly reset the rate limit for a peer (e.g. after trust reset or key rotation).
     */
    fun resetRateLimit(peerId: String) {
        handshakeRateLimits.remove(peerId)
    }

    private fun lazyCleanup() {
        val now = System.currentTimeMillis()
        // We only scan a small portion if it's synchronized to avoid blocking,
        // but since we rely on LinkedHashMap to bound memory,
        // we can just remove expired entries occasionally.
        synchronized(nonceCache) {
            val iterator = nonceCache.entries.iterator()
            var checked = 0
            while (iterator.hasNext() && checked < 20) { // Limit cleanup work per call
                val entry = iterator.next()
                if (now > entry.value) {
                    iterator.remove()
                }
                checked++
            }
        }
    }
}
