package com.meshlink.trust

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentityConflictDetectorTest {

    private val trustStore: TrustStore = mockk(relaxed = true)
    private val auditLog: IdentityAuditLog = mockk(relaxed = true)
    private lateinit var conflictDetector: IdentityConflictDetector

    @Before
    fun setup() {
        conflictDetector = IdentityConflictDetector(trustStore, auditLog)
    }

    @Test
    fun testNoConflictDetected() {
        every { trustStore.getIdentity(any()) } returns null
        every { trustStore.getIdentityByFingerprint(any()) } returns null

        val identity = MeshIdentity(
            meshId = "mesh-123",
            publicKey = "pubkey123",
            displayName = "Alice"
        )

        val result = conflictDetector.detectConflict(identity)
        assertEquals(ConflictType.NONE, result.conflictType)
        assertFalse(result.shouldReject)
    }

    @Test
    fun testMeshIdKeyMismatchReject() {
        val existing = TrustedIdentity(
            meshId = "mesh-123",
            publicKey = "original_key",
            fingerprint = "fp1",
            trustLevel = TrustLevel.VERIFIED,
            verificationMethod = "QR_CODE",
            verificationDate = System.currentTimeMillis()
        )
        every { trustStore.getIdentity("mesh-123") } returns existing

        val incoming = MeshIdentity(
            meshId = "mesh-123",
            publicKey = "different_fake_key",
            displayName = "Impostor Alice"
        )

        val result = conflictDetector.detectConflict(incoming)
        assertEquals(ConflictType.MESH_ID_KEY_MISMATCH_REJECT, result.conflictType)
        assertTrue(result.shouldReject)
    }

    @Test
    fun testDuplicateFingerprintReject() {
        every { trustStore.getIdentity("mesh-new") } returns null

        val existingWithKey = TrustedIdentity(
            meshId = "mesh-existing",
            publicKey = "same_key",
            fingerprint = IdentityFingerprint.compute("same_key"),
            trustLevel = TrustLevel.VERIFIED,
            verificationMethod = "QR_CODE",
            verificationDate = System.currentTimeMillis()
        )
        every { trustStore.getIdentityByFingerprint(any()) } returns existingWithKey

        val incoming = MeshIdentity(
            meshId = "mesh-new",
            publicKey = "same_key",
            displayName = "Duplicate Device"
        )

        val result = conflictDetector.detectConflict(incoming)
        assertEquals(ConflictType.DUPLICATE_FINGERPRINT_REJECT, result.conflictType)
        assertTrue(result.shouldReject)
    }
}
