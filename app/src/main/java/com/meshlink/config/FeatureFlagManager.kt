package com.meshlink.config

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(StorageConfig.PREFS_NAME, Context.MODE_PRIVATE)

    fun isFeatureEnabled(feature: FeatureFlag): Boolean {
        // Developer overrides take precedence in debug/internal builds
        if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) && prefs.contains(feature.key)) {
            return prefs.getBoolean(feature.key, feature.defaultValue)
        }
        return feature.defaultValue
    }

    fun setFeatureEnabled(feature: FeatureFlag, enabled: Boolean) {
        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            prefs.edit().putBoolean(feature.key, enabled).apply()
        }
    }
}

enum class FeatureFlag(val key: String, val defaultValue: Boolean) {
    ENABLE_VOICE_NOTES("feature_voice_notes", true),
    ENABLE_SOS("feature_sos", true),
    ENABLE_ANALYTICS("feature_analytics", true),
    EXPERIMENTAL_ROUTING("feature_experimental_routing", false),
    CLOUD_SYNC_PREVIEW("feature_cloud_sync", false)
}
