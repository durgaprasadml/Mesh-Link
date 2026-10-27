package com.meshlink.wifi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiStreamHostResolverTest {

    @Test
    fun `resolves direct IP address match`() {
        val host = WifiStreamHostResolver.resolve(
            requested = "192.168.49.2",
            connectedHosts = setOf("192.168.49.1", "192.168.49.2"),
            peerHostsByMeshId = emptyMap(),
            canonicalize = { it }
        )
        assertEquals("192.168.49.2", host)
    }

    @Test
    fun `resolves mapped mesh ID`() {
        val host = WifiStreamHostResolver.resolve(
            requested = "peer_alice",
            connectedHosts = setOf("192.168.49.1", "192.168.49.2"),
            peerHostsByMeshId = mapOf("peer_alice" to "192.168.49.2"),
            canonicalize = { it }
        )
        assertEquals("192.168.49.2", host)
    }

    @Test
    fun `resolves canonicalized mesh ID`() {
        val host = WifiStreamHostResolver.resolve(
            requested = "USER_ALICE_LONG_ID",
            connectedHosts = setOf("192.168.49.2"),
            peerHostsByMeshId = mapOf("CANON_ALICE" to "192.168.49.2"),
            canonicalize = { if (it.contains("ALICE")) "CANON_ALICE" else it }
        )
        assertEquals("192.168.49.2", host)
    }

    @Test
    fun `returns null and prevents fanout when multiple hosts connected and requested ID unknown`() {
        val host = WifiStreamHostResolver.resolve(
            requested = "peer_unknown",
            connectedHosts = setOf("192.168.49.1", "192.168.49.2", "192.168.49.3"),
            peerHostsByMeshId = emptyMap(),
            canonicalize = { it }
        )
        assertNull(host)
    }

    @Test
    fun `allows sole connected host fallback when unclaimed`() {
        val host = WifiStreamHostResolver.resolve(
            requested = "peer_bob",
            connectedHosts = setOf("192.168.49.2"),
            peerHostsByMeshId = emptyMap(),
            canonicalize = { it }
        )
        assertEquals("192.168.49.2", host)
    }

    @Test
    fun `refuses sole connected host fallback if claimed by a different peer`() {
        val host = WifiStreamHostResolver.resolve(
            requested = "peer_bob",
            connectedHosts = setOf("192.168.49.2"),
            peerHostsByMeshId = mapOf("peer_charlie" to "192.168.49.2"),
            canonicalize = { it }
        )
        assertNull(host)
    }
}
