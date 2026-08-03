package com.meshlink.trust

import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

enum class ConflictType {
    NONE,
    NAME_COLLISION_WARNING,
    MESH_ID_KEY_MISMATCH_REJECT,
    DUPLICATE_FINGERPRINT_REJECT,
    VALID_KEY_ROTATION
}

data class ConflictResult(
    val conflictType: ConflictType,
    val isActionable: Boolean,
    val shouldReject: Boolean,
    val description: String
)

/**
 * Impersonation and identity conflict detection engine.
 */
@Singleton
class IdentityConflictDetector @Inject constructor(
    private val trustStore: TrustStore,
    private val auditLog: IdentityAuditLog
) {
    companion object {
        private const val TAG = "IdentityConflictDetector"
    }

    /**
     * Checks an incoming identity against known contacts to detect impersonation or key conflicts.
     */
    fun detectConflict(incomingIdentity: MeshIdentity): ConflictResult {
        val incomingFingerprint = IdentityFingerprint.compute(incomingIdentity.publicKey)

        // Scenario 1: Same Mesh ID, check public key matching
        val existingByMeshId = trustStore.getIdentity(incomingIdentity.meshId)
        if (existingByMeshId != null) {
            if (existingByMeshId.publicKey.isNotEmpty() && existingByMeshId.publicKey != incomingIdentity.publicKey) {
                val description = "Mesh ID ${incomingIdentity.meshId} presented conflicting public key"
                MeshLogger.w(TAG, "CONFLICT: $description")
                auditLog.logEvent(AuditEventType.CONFLICT_DETECTED, incomingIdentity.meshId, description)
                return ConflictResult(
                    conflictType = ConflictType.MESH_ID_KEY_MISMATCH_REJECT,
                    isActionable = true,
                    shouldReject = true,
                    description = description
                )
            }
        }

        // Scenario 2: Duplicate Fingerprint for different Mesh ID
        val existingByFingerprint = trustStore.getIdentityByFingerprint(incomingFingerprint)
        if (existingByFingerprint != null && existingByFingerprint.meshId != incomingIdentity.meshId) {
            val description = "Fingerprint $incomingFingerprint reused by different Mesh ID ${incomingIdentity.meshId}"
            MeshLogger.w(TAG, "CONFLICT: $description")
            auditLog.logEvent(AuditEventType.CONFLICT_DETECTED, incomingIdentity.meshId, description)
            return ConflictResult(
                conflictType = ConflictType.DUPLICATE_FINGERPRINT_REJECT,
                isActionable = true,
                shouldReject = true,
                description = description
            )
        }

        // Scenario 3: Same Display Name with different public keys (Warning)
        val allIdentities = trustStore.getAllIdentities()
        val nameCollision = allIdentities.any {
            it.notes?.contains(incomingIdentity.displayName, ignoreCase = true) == true &&
                    it.meshId != incomingIdentity.meshId
        }
        if (nameCollision) {
            val description = "Display name '${incomingIdentity.displayName}' matches another peer with different key"
            MeshLogger.w(TAG, "WARNING: $description")
            auditLog.logEvent(AuditEventType.CONFLICT_DETECTED, incomingIdentity.meshId, description)
            return ConflictResult(
                conflictType = ConflictType.NAME_COLLISION_WARNING,
                isActionable = true,
                shouldReject = false,
                description = description
            )
        }

        return ConflictResult(
            conflictType = ConflictType.NONE,
            isActionable = false,
            shouldReject = false,
            description = "No conflict detected"
        )
    }
}
