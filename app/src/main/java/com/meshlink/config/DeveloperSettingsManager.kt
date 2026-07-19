package com.meshlink.config

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperSettingsManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(StorageConfig.PREFS_NAME, Context.MODE_PRIVATE)

    fun isVerboseLoggingEnabled(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) && prefs.getBoolean("dev_verbose_logging", false)
    }

    fun isForceBleOnly(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) && prefs.getBoolean("dev_force_ble", false)
    }

    fun isForceWifiOnly(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) && prefs.getBoolean("dev_force_wifi", false)
    }
}
