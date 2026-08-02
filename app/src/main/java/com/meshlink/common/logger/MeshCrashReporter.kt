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
        crashlytics.setUserId(userId)
    }

    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
    }
}
