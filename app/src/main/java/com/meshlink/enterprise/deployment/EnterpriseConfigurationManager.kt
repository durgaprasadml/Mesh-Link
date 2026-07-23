package com.meshlink.enterprise.deployment

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EnterpriseConfig(
    val disableMediaTransfers: Boolean = false,
    val forceEmergencyMode: Boolean = false,
    val enforceEncryptedBackups: Boolean = true,
    val disableAnalytics: Boolean = false
)

@Singleton
class EnterpriseConfigurationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _configFlow = MutableStateFlow(EnterpriseConfig())
    val configFlow: StateFlow<EnterpriseConfig> = _configFlow

    fun reloadManagedConfigurations() {
        val restrictionsManager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
        
        // This bundle is populated by MDMs like Intune or Workspace ONE pushing XML key-value pairs
        val restrictions: Bundle? = restrictionsManager?.applicationRestrictions
        
        if (restrictions != null && !restrictions.isEmpty) {
            val newConfig = EnterpriseConfig(
                disableMediaTransfers = restrictions.getBoolean("disable_media_transfers", false),
                forceEmergencyMode = restrictions.getBoolean("force_emergency_mode", false),
                enforceEncryptedBackups = restrictions.getBoolean("enforce_encrypted_backups", true),
                disableAnalytics = restrictions.getBoolean("disable_analytics", false)
            )
            _configFlow.value = newConfig
        }
    }
}
