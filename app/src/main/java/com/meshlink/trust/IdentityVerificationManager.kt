package com.meshlink.trust

import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

enum class VerificationMethod {
    QR_CODE,
    FINGERPRINT,
    MANUAL,
    TRUSTED_IMPORT
}

sealed class VerificationResult {
    data class Success(val identity: MeshIdentity, val trustLevel: TrustLevel) : VerificationResult()
    data class Failure(val reason: String, val conflictResult: ConflictResult? = null) : VerificationResult()
}

/**
 * Offline identity verification engine.
 */
@Singleton
class IdentityVerificationManager @Inject constructor(
    private val trustStore: TrustStore,
    private val conflictDetector: IdentityConflictDetector,
    private val auditLog: IdentityAuditLog,
    private val identityManager: MeshIdentityManager
) {
    companion object {
        private const val TAG = "IdentityVerificationManager"
    }

    /**
     * Verifies identity cryptographic integrity, checks conflicts, and updates TrustStore.
     */
    fun verifyIdentity(
        targetIdentity: MeshIdentity,
        method: VerificationMethod,
        targetLevel: TrustLevel = TrustLevel.VERIFIED,
        notes: String? = null
    ): VerificationResult {
        // 1. Detect conflicts
        val conflict = conflictDetector.detectConflict(targetIdentity)
        if (conflict.shouldReject) {
            MeshLogger.w(TAG, "Identity verification rejected due to conflict: ${conflict.description}")
            return VerificationResult.Failure("Conflict detected: ${conflict.description}", conflict)
        }

        // 2. Validate cryptographic signature if provided
        if (targetIdentity.signature.isNotEmpty()) {
            val sigValid = identityManager.verifyRemoteIdentity(targetIdentity)
            if (!sigValid) {
                val reason = "Invalid identity signature for Mesh ID ${targetIdentity.meshId}"
                MeshLogger.w(TAG, reason)
                auditLog.logEvent(AuditEventType.CONFLICT_DETECTED, targetIdentity.meshId, reason)
                return VerificationResult.Failure(reason)
            }
        }

        // 3. Promote trust level in TrustStore
        val finalLevel = if (targetLevel == TrustLevel.UNKNOWN) TrustLevel.VERIFIED else targetLevel
        trustStore.updateTrust(
            meshId = targetIdentity.meshId,
            publicKey = targetIdentity.publicKey,
            trustLevel = finalLevel,
            verificationMethod = method.name,
            notes = notes ?: "Verified via ${method.name}"
        )

        // 4. Record audit log entry
        auditLog.logEvent(
            eventType = AuditEventType.IDENTITY_VERIFIED,
            meshId = targetIdentity.meshId,
            details = "Verified via ${method.name} with level ${finalLevel.name}"
        )

        MeshLogger.i(TAG, "Successfully verified identity ${targetIdentity.meshId} via ${method.name}")
        return VerificationResult.Success(targetIdentity, finalLevel)
    }

    /**
     * Manually revokes trust for a peer.
     */
    fun revokeVerification(meshId: String, reason: String): Boolean {
        trustStore.revokeTrust(meshId)
        auditLog.logEvent(
            eventType = AuditEventType.VERIFICATION_REVOKED,
            meshId = meshId,
            details = "Verification revoked: $reason"
        )
        return true
    }
}
