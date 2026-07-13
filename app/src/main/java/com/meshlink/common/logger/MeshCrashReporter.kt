package com.meshlink.common.logger

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun logNonFatal(throwable: Throwable, metadata: Map<String, String>? = null) {
        // TODO: In a production Firebase environment, you would use:
        // FirebaseCrashlytics.getInstance().recordException(throwable)
        // For now, we simulate this structured logging internally.
        
        MeshLogger.e("CrashReporter", "Non-fatal exception recorded: ${throwable.message}")
        metadata?.forEach { (key, value) ->
            MeshLogger.e("CrashReporter", "Custom Key: $key = $value")
            // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        }
    }

    fun setUserId(userId: String) {
        // FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun logBreadcrumb(message: String) {
        // FirebaseCrashlytics.getInstance().log(message)
    }
}
