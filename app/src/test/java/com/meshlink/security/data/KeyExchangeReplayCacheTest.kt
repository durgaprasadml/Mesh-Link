package com.meshlink.security.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class KeyExchangeReplayCacheTest {

    private lateinit var cache: KeyExchangeReplayCache

    @Before
    fun setup() {
        cache = KeyExchangeReplayCache()
    }

    @Test
    fun `isReplay returns false for fresh nonce`() {
        val peerId = "peer123"
        val nonce = UUID.randomUUID().toString()
        assertFalse(cache.isReplay(peerId, nonce))
    }

    @Test
    fun `isReplay returns true for duplicate nonce`() {
        val peerId = "peer123"
        val nonce = UUID.randomUUID().toString()
        
        cache.recordNonce(peerId, nonce)
        
        assertTrue(cache.isReplay(peerId, nonce))
    }

    @Test
    fun `isReplay returns false for same nonce different peer`() {
        val nonce = UUID.randomUUID().toString()
        
        cache.recordNonce("peer1", nonce)
        
        // Peer 2 generates identical nonce (unlikely, but validates isolation)
        assertFalse(cache.isReplay("peer2", nonce))
    }

    @Test
    fun `rate limit blocks rapid handshakes`() {
        val peerId = "peer123"
        
        assertTrue("First handshake should be allowed", cache.canProcessHandshake(peerId))
        cache.recordHandshake(peerId)
        
        assertFalse("Second immediate handshake should be blocked", cache.canProcessHandshake(peerId))
    }

    @Test
    fun `rate limit bypass works via reset`() {
        val peerId = "peer123"
        
        cache.recordHandshake(peerId)
        assertFalse(cache.canProcessHandshake(peerId))
        
        cache.resetRateLimit(peerId)
        assertTrue("Handshake should be allowed after reset", cache.canProcessHandshake(peerId))
    }
    
    @Test
    fun `cache memory bound is enforced`() {
        val peerId = "peerX"
        val maxItems = SecurityConstants.KEY_EXCHANGE_REPLAY_CACHE_SIZE
        
        // Insert more than maxItems
        for (i in 1..(maxItems + 10)) {
            cache.recordNonce(peerId, "nonce$i")
        }
        
        // The first 10 items should have been evicted
        assertFalse("Oldest entry should be evicted", cache.isReplay(peerId, "nonce1"))
        
        // The latest item should still be present
        assertTrue("Newest entry should be present", cache.isReplay(peerId, "nonce${maxItems + 10}"))
    }
}
