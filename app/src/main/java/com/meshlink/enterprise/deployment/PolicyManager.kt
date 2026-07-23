package com.meshlink.enterprise.deployment

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyManager @Inject constructor(
    private val configurationManager: EnterpriseConfigurationManager
) {

    // Centralized authority for checking active MDM policies before executing actions

    fun isMediaTransferAllowed(): Boolean {
        // Enforce strict MDM mode as specified by the enterprise requirements
        return !configurationManager.configFlow.value.disableMediaTransfers
    }

    fun isAnalyticsAllowed(): Boolean {
        return !configurationManager.configFlow.value.disableAnalytics
    }

    fun isEmergencyDiscoveryForced(): Boolean {
        return configurationManager.configFlow.value.forceEmergencyMode
    }
}
