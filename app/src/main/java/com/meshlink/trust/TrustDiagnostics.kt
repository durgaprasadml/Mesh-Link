package com.meshlink.trust

import javax.inject.Inject
import javax.inject.Singleton

data class TrustDiagnosticReport(
    val totalVerifiedUsers: Int,
    val totalUnknownUsers: Int,
    val activeCommunitiesCount: Int,
    val verificationSuccessCount: Int,
    val conflictCount: Int,
    val revokedUsersCount: Int,
    val suspiciousActivityCount: Int,
    val auditLogHealth: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Structured diagnostics provider for system trust and identity state.
 */
@Singleton
class TrustDiagnostics @Inject constructor(
    private val trustStore: TrustStore,
    private val auditLog: IdentityAuditLog,
    private val membershipManager: CommunityMembershipManager
) {

    fun generateReport(): TrustDiagnosticReport {
        val identities = trustStore.getAllIdentities()
        val verified = identities.count { it.trustLevel.isVerified() }
        val unknown = identities.count { it.trustLevel == TrustLevel.UNKNOWN || it.trustLevel == TrustLevel.DISCOVERED }
        val revoked = identities.count { it.trustLevel == TrustLevel.REVOKED || it.trustLevel == TrustLevel.BLOCKED }

        val entries = auditLog.getEntries()
        val verifications = entries.count { it.eventType == AuditEventType.IDENTITY_VERIFIED }
        val conflicts = entries.count { it.eventType == AuditEventType.CONFLICT_DETECTED }
        val chainHealthy = auditLog.verifyChainIntegrity()

        return TrustDiagnosticReport(
            totalVerifiedUsers = verified,
            totalUnknownUsers = unknown,
            activeCommunitiesCount = 0,
            verificationSuccessCount = verifications,
            conflictCount = conflicts,
            revokedUsersCount = revoked,
            suspiciousActivityCount = conflicts,
            auditLogHealth = chainHealthy
        )
    }
}
