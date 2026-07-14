package com.meshlink.database.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TrustEntityTest {

    @Test
    fun `test trust entity fields and equality`() {
        val trust1 = TrustEntity(
            peerId = "peer_1",
            deviceUUID = "uuid",
            fingerprint = "fp",
            firstSeen = 100L,
            lastSeen = 200L,
            lastIPAddress = "192.168.1.1",
            lastBLEAddress = "AA:BB:CC",
            keyVersion = 1,
            trustLevel = "TRUSTED",
            verificationStatus = "VERIFIED",
            trustScore = 100,
            identityHistory = "[]"
        )
        val trust2 = trust1.copy()
        val trust3 = trust1.copy(peerId = "peer_2")

        assertEquals("peer_1", trust1.peerId)
        assertEquals("TRUSTED", trust1.trustLevel)
        assertEquals(trust1, trust2)
        assertEquals(trust1.hashCode(), trust2.hashCode())
        assertNotEquals(trust1, trust3)
    }
}
