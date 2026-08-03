package com.meshlink.trust

/**
 * Capability matrix evaluating actions permitted for a peer based on Trust Level and context.
 */
data class TrustCapabilities(
    val canBroadcast: Boolean,
    val canDirectMessage: Boolean,
    val canJoinCommunity: Boolean,
    val canRelayMessages: Boolean,
    val canModerateCommunity: Boolean,
    val canAdministerCommunity: Boolean,
    val canSendEmergencyAlert: Boolean,
    val canReceiveEmergencyAlert: Boolean,
    val showWarning: Boolean,
    val warningReason: String? = null
) {
    companion object {
        fun fullAccess(): TrustCapabilities = TrustCapabilities(
            canBroadcast = true,
            canDirectMessage = true,
            canJoinCommunity = true,
            canRelayMessages = true,
            canModerateCommunity = true,
            canAdministerCommunity = true,
            canSendEmergencyAlert = true,
            canReceiveEmergencyAlert = true,
            showWarning = false
        )

        fun restricted(reason: String): TrustCapabilities = TrustCapabilities(
            canBroadcast = false,
            canDirectMessage = false,
            canJoinCommunity = false,
            canRelayMessages = false,
            canModerateCommunity = false,
            canAdministerCommunity = false,
            canSendEmergencyAlert = false,
            canReceiveEmergencyAlert = true,
            showWarning = true,
            warningReason = reason
        )
    }
}
