package com.meshlink.common.logger

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun logNonFatal(throwable: Throwable, metadata: Map<String, String>? = null) {
        MeshLogger.e("CrashReporter", "Non-fatal exception recorded: ${throwable.message}")
        
        metadata?.forEach { (key, value) ->
            MeshLogger.e("CrashReporter", "Custom Key: $key = $value")
        }
    }

    fun setUserId(userId: String) {
    }

    fun logBreadcrumb(message: String) {
    }
}
