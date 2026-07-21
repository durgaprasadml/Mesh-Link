package com.meshlink.common.logger

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.crashlytics.FirebaseCrashlytics

@Singleton
class MeshCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun logNonFatal(throwable: Throwable, metadata: Map<String, String>? = null) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
        
        MeshLogger.e("CrashReporter", "Non-fatal exception recorded: ${throwable.message}")
        
        metadata?.forEach { (key, value) ->
            MeshLogger.e("CrashReporter", "Custom Key: $key = $value")
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
    }

    fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun logBreadcrumb(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }
}
