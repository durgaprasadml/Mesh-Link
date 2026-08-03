package com.meshlink.trust

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentityVerificationManagerTest {

    private val trustStore: TrustStore = mockk(relaxed = true)
    private val conflictDetector: IdentityConflictDetector = mockk(relaxed = true)
    private val auditLog: IdentityAuditLog = mockk(relaxed = true)
    private val identityManager: MeshIdentityManager = mockk(relaxed = true)

    private lateinit var verificationManager: IdentityVerificationManager

    @Before
    fun setup() {
        verificationManager = IdentityVerificationManager(
            trustStore = trustStore,
            conflictDetector = conflictDetector,
            auditLog = auditLog,
            identityManager = identityManager
        )
    }

    @Test
    fun testSuccessfulVerification() {
        every { conflictDetector.detectConflict(any()) } returns ConflictResult(
            conflictType = ConflictType.NONE,
            isActionable = false,
            shouldReject = false,
            description = "No conflict"
        )
        every { identityManager.verifyRemoteIdentity(any()) } returns true

        val identity = MeshIdentity(
            meshId = "mesh-456",
            publicKey = "valid_key",
            displayName = "Bob",
            signature = "valid_sig"
        )

        val result = verificationManager.verifyIdentity(
            targetIdentity = identity,
            method = VerificationMethod.QR_CODE,
            targetLevel = TrustLevel.VERIFIED
        )

        assertTrue(result is VerificationResult.Success)
    }
}
