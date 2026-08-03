package com.meshlink.trust

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class MembershipStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED,
    REVOKED
}

data class CommunityMember(
    val meshId: String,
    val communityId: String,
    val role: CommunityRole = CommunityRole.MEMBER,
    val status: MembershipStatus = MembershipStatus.PENDING,
    val joinedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Manager for community membership state and governance.
 */
@Singleton
class CommunityMembershipManager @Inject constructor(
    private val trustStore: TrustStore,
    private val auditLog: IdentityAuditLog
) {
    companion object {
        private const val TAG = "CommunityMembershipManager"
    }

    // Community ID -> Map<MeshID, CommunityMember>
    private val communityStore = ConcurrentHashMap<String, ConcurrentHashMap<String, CommunityMember>>()
    private val communityPolicies = ConcurrentHashMap<String, CommunityPolicy>()

    fun setCommunityPolicy(communityId: String, policy: CommunityPolicy) {
        communityPolicies[communityId] = policy
    }

    fun getCommunityPolicy(communityId: String): CommunityPolicy {
        return communityPolicies[communityId] ?: CommunityPolicy.defaultPolicy(CommunityType.ORGANIZATION)
    }

    fun joinCommunity(
        meshId: String,
        communityId: String,
        requestedRole: CommunityRole = CommunityRole.MEMBER
    ): Boolean {
        val policy = getCommunityPolicy(communityId)
        val trustLevel = trustStore.getTrustLevel(meshId)

        if (policy.requireVerifiedTrust && !trustLevel.isVerified()) {
            MeshLogger.w(TAG, "Join rejected for $meshId: Policy requires verified trust level")
            return false
        }

        val initialStatus = if (policy.requireAdminApproval) MembershipStatus.PENDING else MembershipStatus.APPROVED
        val member = CommunityMember(
            meshId = meshId,
            communityId = communityId,
            role = requestedRole,
            status = initialStatus
        )

        val membersMap = communityStore.computeIfAbsent(communityId) { ConcurrentHashMap() }
        membersMap[meshId] = member

        auditLog.logEvent(
            eventType = AuditEventType.COMMUNITY_JOINED,
            meshId = meshId,
            details = "Joined community $communityId with role ${requestedRole.name}, status ${initialStatus.name}"
        )
        return true
    }

    fun updateMemberStatus(
        adminMeshId: String,
        communityId: String,
        targetMeshId: String,
        newStatus: MembershipStatus
    ): Boolean {
        val membersMap = communityStore[communityId] ?: return false
        val admin = membersMap[adminMeshId] ?: return false
        if (!admin.role.hasAdminPrivileges()) {
            MeshLogger.w(TAG, "Status update failed: Admin $adminMeshId lacks admin privileges")
            return false
        }

        val target = membersMap[targetMeshId] ?: return false
        val updated = target.copy(status = newStatus)
        membersMap[targetMeshId] = updated

        auditLog.logEvent(
            eventType = AuditEventType.COMMUNITY_JOINED,
            meshId = targetMeshId,
            details = "Membership status updated to ${newStatus.name} by admin $adminMeshId in $communityId"
        )
        return true
    }

    fun getMembers(communityId: String): List<CommunityMember> {
        return communityStore[communityId]?.values?.toList() ?: emptyList()
    }

    fun getMember(communityId: String, meshId: String): CommunityMember? {
        return communityStore[communityId]?.get(meshId)
    }

    fun isMemberApproved(communityId: String, meshId: String): Boolean {
        val member = getMember(communityId, meshId) ?: return false
        return member.status == MembershipStatus.APPROVED
    }
}
