package com.meshlink.securitytest

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PeerSecureSession
import com.meshlink.domain.model.SessionState
import com.meshlink.security.policy.EncryptionRequirement
import com.meshlink.security.policy.PacketEncryptionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshSecurityHardenTest {

    @Test
    fun `test packet encryption policy enforces mandatory encryption for all user payloads`() {
        assertEquals(EncryptionRequirement.REQUIRED, PacketEncryptionPolicy.getRequirement(PacketType.TEXT))
        assertEquals(EncryptionRequirement.REQUIRED, PacketEncryptionPolicy.getRequirement(PacketType.LOCATION))
        assertEquals(EncryptionRequirement.REQUIRED, PacketEncryptionPolicy.getRequirement(PacketType.MEDIA_CHUNK))
        assertEquals(EncryptionRequirement.REQUIRED, PacketEncryptionPolicy.getRequirement(PacketType.VOICE_FRAME))
        assertEquals(EncryptionRequirement.REQUIRED, PacketEncryptionPolicy.getRequirement(PacketType.SOS))

        assertEquals(EncryptionRequirement.BOOTSTRAP_ONLY, PacketEncryptionPolicy.getRequirement(PacketType.KEY_EXCHANGE))
        assertEquals(EncryptionRequirement.BOOTSTRAP_ONLY, PacketEncryptionPolicy.getRequirement(PacketType.BEACON))
        assertEquals(EncryptionRequirement.BOOTSTRAP_ONLY, PacketEncryptionPolicy.getRequirement(PacketType.ROUTE_REQUEST))

        val unencryptedLocationPacket = MeshPacket(
            senderId = "ALICE",
            targetId = "BOB",
            payload = "{\"lat\":37.77,\"lng\":-122.41}",
            type = PacketType.LOCATION,
            encrypted = false
        )
        assertFalse("Unencrypted location packet must be rejected by policy",
            PacketEncryptionPolicy.validatePacketEncryption(unencryptedLocationPacket, strictMode = true, hasSecureSession = true))

        val unencryptedBroadcastPacket = MeshPacket(
            senderId = "ALICE",
            targetId = "BROADCAST",
            payload = "Emergency broadcast",
            type = PacketType.TEXT,
            encrypted = false
        )
        assertFalse("Unencrypted broadcast text packet must be rejected by policy",
            PacketEncryptionPolicy.validatePacketEncryption(unencryptedBroadcastPacket, strictMode = true, hasSecureSession = true))
    }

    @Test
    fun `test session state transitions and activity update`() {
        val session = PeerSecureSession(
            peerId = "BOB",
            sessionId = "sess_12345",
            fingerprint = "FP_BOB",
            sessionStart = System.currentTimeMillis(),
            sessionVersion = 2,
            verified = true,
            lastActivity = System.currentTimeMillis(),
            state = SessionState.CREATING
        )

        assertEquals(SessionState.CREATING, session.state)

        session.state = SessionState.ACTIVE
        assertEquals(SessionState.ACTIVE, session.state)

        session.state = SessionState.REKEYING
        assertEquals(SessionState.REKEYING, session.state)

        val now = System.currentTimeMillis()
        session.updateActivity(now)
        assertEquals(now, session.lastActivity)
        assertTrue(session.expirationTime > now)
    }

    @Test
    fun `test sliding window and packet ID replay protection`() {
        val session = PeerSecureSession(
            peerId = "BOB",
            sessionId = "sess_999",
            fingerprint = "FP_BOB",
            sessionStart = System.currentTimeMillis(),
            sessionVersion = 2,
            verified = true,
            lastActivity = System.currentTimeMillis()
        )

        // First packet seq = 1
        assertFalse("Initial sequence 1 should not be a replay", session.isReplay(1, "pkt_101"))
        session.markReceived(1, "pkt_101")

        // Duplicate seq = 1
        assertTrue("Duplicate sequence 1 must be flagged as replay", session.isReplay(1, "pkt_101"))

        // Duplicate packet ID
        assertTrue("Duplicate packet ID must be flagged as replay", session.isReplay(5, "pkt_101"))

        // Next sequence seq = 2
        assertFalse("Sequence 2 should not be a replay", session.isReplay(2, "pkt_102"))
        session.markReceived(2, "pkt_102")

        // Out-of-order within window seq = 3
        assertFalse("Sequence 3 should be accepted", session.isReplay(3, "pkt_103"))
        session.markReceived(3, "pkt_103")

        // Out-of-window old sequence (seq = -100)
        assertTrue("Sequence outside 64-bit window must be rejected", session.isReplay(-100, "pkt_old"))
    }
}
