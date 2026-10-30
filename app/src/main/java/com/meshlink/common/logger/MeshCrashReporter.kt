package com.meshlink.common.logger

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    fun logNonFatal(throwable: Throwable, metadata: Map<String, String>? = null) {
        MeshLogger.e("CrashReporter", "Non-fatal exception recorded: ${throwable.message}")
        
        metadata?.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
            MeshLogger.e("CrashReporter", "Custom Key: $key = $value")
        }
        crashlytics.recordException(throwable)
    }

    fun setUserId(userId: String) {
        val sanitized = sanitizeIdentifier(userId)
        if (sanitized.isNotBlank()) {
            crashlytics.setUserId(sanitized)
        }
    }

    fun initializeAnonymousIdentity(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ANON_ID, null)
        val anonId = if (!existing.isNullOrBlank() && !existing.contains("@")) {
            existing
        } else {
            "anon-${java.util.UUID.randomUUID().toString().replace("-", "").take(12)}".also {
                prefs.edit().putString(KEY_ANON_ID, it).apply()
            }
        }
        crashlytics.setUserId(anonId)
        return anonId
    }

    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
    }

    companion object {
        private const val PREFS_NAME = "mesh_crash_reporter_prefs"
        private const val KEY_ANON_ID = "anonymous_crash_id"

        internal fun sanitizeIdentifier(rawId: String): String {
            val trimmed = rawId.trim()
            if (trimmed.isBlank() || trimmed.contains("@")) {
                return ""
            }
            return trimmed
        }
    }
}
