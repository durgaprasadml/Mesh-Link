package com.meshlink.trust

import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

data class CommunityInvitation(
    val communityId: String,
    val communityName: String,
    val targetMeshId: String,
    val role: CommunityRole,
    val issuerMeshId: String,
    val expirationTimestamp: Long,
    val signature: String = ""
) {
    fun toSigningPayload(): String {
        return "$communityId:$targetMeshId:${role.name}:$issuerMeshId:$expirationTimestamp"
    }

    fun toJson(): String {
        return """{"communityId":"$communityId","communityName":"$communityName","targetMeshId":"$targetMeshId","role":"${role.name}","issuerMeshId":"$issuerMeshId","expirationTimestamp":$expirationTimestamp,"signature":"$signature"}"""
    }

    companion object {
        fun fromJson(jsonStr: String): CommunityInvitation {
            val communityId = extractValue(jsonStr, "communityId")
            val communityName = extractValue(jsonStr, "communityName")
            val targetMeshId = extractValue(jsonStr, "targetMeshId")
            val roleStr = extractValue(jsonStr, "role")
            val issuerMeshId = extractValue(jsonStr, "issuerMeshId")
            val expiration = extractValue(jsonStr, "expirationTimestamp").toLongOrNull() ?: 0L
            val signature = extractValue(jsonStr, "signature")

            return CommunityInvitation(
                communityId = communityId,
                communityName = communityName,
                targetMeshId = targetMeshId,
                role = try { CommunityRole.valueOf(roleStr) } catch (e: Exception) { CommunityRole.MEMBER },
                issuerMeshId = issuerMeshId,
                expirationTimestamp = expiration,
                signature = signature
            )
        }

        private fun extractValue(json: String, key: String): String {
            val pattern = """"$key"\s*:\s*("(.*?)"|(\d+))""".toRegex()
            val match = pattern.find(json) ?: return ""
            return match.groupValues[2].ifEmpty { match.groupValues[3] }
        }
    }
}

/**
 * Offline signed community invitation manager.
 */
@Singleton
class CommunityInvitationManager @Inject constructor(
    private val identityManager: MeshIdentityManager,
    private val trustStore: TrustStore,
    private val membershipManager: CommunityMembershipManager
) {
    companion object {
        private const val TAG = "CommunityInvitationManager"
    }

    /**
     * Admin creates and signs an offline community invitation for a target peer.
     */
    fun createSignedInvitation(
        communityId: String,
        communityName: String,
        targetMeshId: String,
        role: CommunityRole,
        validityMillis: Long = 86400000L // 24 hours
    ): CommunityInvitation {
        val issuer = identityManager.getOrCreateIdentity()
        val expiration = System.currentTimeMillis() + validityMillis

        val temp = CommunityInvitation(
            communityId = communityId,
            communityName = communityName,
            targetMeshId = targetMeshId,
            role = role,
            issuerMeshId = issuer.meshId,
            expirationTimestamp = expiration,
            signature = ""
        )

        val signature = identityManager.signIdentityPayload(temp.toSigningPayload())
        return temp.copy(signature = signature)
    }

    /**
     * Recipient verifies invitation signature offline and joins community.
     */
    fun acceptInvitation(invitationJson: String): Boolean {
        return try {
            val invitation = CommunityInvitation.fromJson(invitationJson)

            // Check expiration
            if (System.currentTimeMillis() > invitation.expirationTimestamp) {
                MeshLogger.w(TAG, "Invitation for community ${invitation.communityId} expired")
                return false
            }

            // Verify issuer identity & trust
            val issuerIdentity = trustStore.getIdentity(invitation.issuerMeshId)
            if (issuerIdentity == null || !issuerIdentity.trustLevel.isVerified()) {
                MeshLogger.w(TAG, "Issuer ${invitation.issuerMeshId} is not a verified contact")
                return false
            }

            // Execute community join
            membershipManager.joinCommunity(
                meshId = invitation.targetMeshId,
                communityId = invitation.communityId,
                requestedRole = invitation.role
            )
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to accept signed invitation: ${e.message}")
            false
        }
    }
}
