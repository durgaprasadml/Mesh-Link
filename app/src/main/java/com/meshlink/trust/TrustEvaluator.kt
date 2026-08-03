package com.meshlink.trust

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trust Evaluation Engine producing TrustCapabilities.
 */
@Singleton
class TrustEvaluator @Inject constructor(
    private val trustStore: TrustStore
) {

    /**
     * Evaluates trust capabilities for a peer ID given optional community context.
     */
    fun evaluate(
        peerMeshId: String,
        communityId: String? = null,
        role: CommunityRole? = null,
        policy: CommunityPolicy? = null
    ): TrustCapabilities {
        val trustLevel = trustStore.getTrustLevel(peerMeshId)
        return evaluateLevel(trustLevel, role, policy)
    }

    /**
     * Core evaluation logic given explicit TrustLevel, CommunityRole, and CommunityPolicy.
     */
    fun evaluateLevel(
        trustLevel: TrustLevel,
        role: CommunityRole? = null,
        policy: CommunityPolicy? = null
    ): TrustCapabilities {
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            return TrustCapabilities.restricted("Peer is explicitly blocked or revoked")
        }

        val effectivePolicy = policy ?: CommunityPolicy.defaultPolicy(CommunityType.ORGANIZATION)

        // Policy checks for unknown users
        if (trustLevel == TrustLevel.UNKNOWN || trustLevel == TrustLevel.DISCOVERED) {
            if (effectivePolicy.requireVerifiedTrust) {
                return TrustCapabilities(
                    canBroadcast = !effectivePolicy.restrictUnknownBroadcasts,
                    canDirectMessage = false,
                    canJoinCommunity = false,
                    canRelayMessages = false,
                    canModerateCommunity = false,
                    canAdministerCommunity = false,
                    canSendEmergencyAlert = false,
                    canReceiveEmergencyAlert = true,
                    showWarning = true,
                    warningReason = "Unverified peer in restricted community policy"
                )
            }

            return TrustCapabilities(
                canBroadcast = true,
                canDirectMessage = true,
                canJoinCommunity = false,
                canRelayMessages = true,
                canModerateCommunity = false,
                canAdministerCommunity = false,
                canSendEmergencyAlert = false,
                canReceiveEmergencyAlert = true,
                showWarning = true,
                warningReason = "Unverified peer"
            )
        }

        val isAdmin = role?.hasAdminPrivileges() == true || trustLevel == TrustLevel.ADMIN || trustLevel == TrustLevel.EMERGENCY_AUTHORITY
        val isEmergency = role == CommunityRole.EMERGENCY_AUTHORITY || trustLevel == TrustLevel.EMERGENCY_AUTHORITY

        return TrustCapabilities(
            canBroadcast = true,
            canDirectMessage = true,
            canJoinCommunity = trustLevel.isVerified(),
            canRelayMessages = true,
            canModerateCommunity = isAdmin || role?.canModerate() == true,
            canAdministerCommunity = isAdmin,
            canSendEmergencyAlert = isEmergency || trustLevel.isVerified(),
            canReceiveEmergencyAlert = true,
            showWarning = false,
            warningReason = null
        )
    }
}
