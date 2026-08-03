package com.meshlink.trust

enum class CommunityType {
    HOSTEL,
    CLASSROOM,
    COLLEGE,
    ORGANIZATION,
    RESCUE_TEAM,
    EVENT
}

/**
 * Configurable policy rules for community membership governance.
 */
data class CommunityPolicy(
    val communityType: CommunityType,
    val requireVerifiedTrust: Boolean,
    val requireAdminApproval: Boolean,
    val hideUnknownUsers: Boolean,
    val restrictUnknownBroadcasts: Boolean,
    val minRequiredRoleToInvite: CommunityRole = CommunityRole.ADMIN
) {
    companion object {
        fun hostelPolicy(): CommunityPolicy = CommunityPolicy(
            communityType = CommunityType.HOSTEL,
            requireVerifiedTrust = true,
            requireAdminApproval = true,
            hideUnknownUsers = true,
            restrictUnknownBroadcasts = true,
            minRequiredRoleToInvite = CommunityRole.ADMIN
        )

        fun collegePolicy(): CommunityPolicy = CommunityPolicy(
            communityType = CommunityType.COLLEGE,
            requireVerifiedTrust = true,
            requireAdminApproval = true,
            hideUnknownUsers = false,
            restrictUnknownBroadcasts = false,
            minRequiredRoleToInvite = CommunityRole.MODERATOR
        )

        fun emergencyPolicy(): CommunityPolicy = CommunityPolicy(
            communityType = CommunityType.RESCUE_TEAM,
            requireVerifiedTrust = true,
            requireAdminApproval = false,
            hideUnknownUsers = false,
            restrictUnknownBroadcasts = true,
            minRequiredRoleToInvite = CommunityRole.EMERGENCY_AUTHORITY
        )

        fun defaultPolicy(type: CommunityType): CommunityPolicy = CommunityPolicy(
            communityType = type,
            requireVerifiedTrust = false,
            requireAdminApproval = false,
            hideUnknownUsers = false,
            restrictUnknownBroadcasts = false,
            minRequiredRoleToInvite = CommunityRole.MEMBER
        )
    }

    /**
     * Checks if candidate member satisfies policy requirements.
     */
    fun satisfiesPolicy(candidateTrust: TrustLevel, isApproved: Boolean): Boolean {
        if (requireVerifiedTrust && !candidateTrust.isVerified()) return false
        if (requireAdminApproval && !isApproved) return false
        return true
    }
}
