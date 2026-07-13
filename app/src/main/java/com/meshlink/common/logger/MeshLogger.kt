package com.meshlink.common.logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

object MeshLogger {

    private var crashReporter: MeshCrashReporter? = null

    fun init(reporter: MeshCrashReporter) {
        this.crashReporter = reporter
    }

    fun log(
        level: LogLevel,
        category: LogCategory,
        message: String,
        metadata: Map<String, String>? = null,
        t: Throwable? = null
    ) {
        val sanitizedMessage = SanitizationInterceptor.sanitize(message)
        val sanitizedMetadata = SanitizationInterceptor.sanitizeMetadata(metadata)
        
        val metadataString = if (sanitizedMetadata.isNotEmpty()) " | Metadata: $sanitizedMetadata" else ""
        val fullMessage = "[$category] $sanitizedMessage$metadataString"

        when (level) {
            LogLevel.VERBOSE -> Log.v(category.name, fullMessage, t)
            LogLevel.DEBUG -> Log.d(category.name, fullMessage, t)
            LogLevel.INFO -> Log.i(category.name, fullMessage, t)
            LogLevel.WARNING -> Log.w(category.name, fullMessage, t)
            LogLevel.ERROR -> {
                if (t != null) Log.e(category.name, fullMessage, t) else Log.e(category.name, fullMessage)
            }
            LogLevel.CRITICAL -> {
                if (t != null) Log.e(category.name, "CRITICAL: $fullMessage", t) else Log.e(category.name, "CRITICAL: $fullMessage")
                crashReporter?.logNonFatal(t ?: Exception(fullMessage), sanitizedMetadata)
            }
        }
    }

    // Legacy wrappers for backward compatibility during the refactoring process
    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, LogCategory.SYSTEM, "[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, LogCategory.SYSTEM, "[$tag] $message", null, throwable)
    }

    fun w(tag: String, message: String) {
        log(LogLevel.WARNING, LogCategory.SYSTEM, "[$tag] $message")
    }

    fun i(tag: String, message: String) {
        log(LogLevel.INFO, LogCategory.SYSTEM, "[$tag] $message")
    }
}
