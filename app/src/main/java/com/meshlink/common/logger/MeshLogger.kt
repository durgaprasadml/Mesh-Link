package com.meshlink.common.logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

object MeshLogger {

    private var crashReporter: MeshCrashReporter? = null

    fun init(reporter: MeshCrashReporter) {
        this.crashReporter = reporter
    }

    fun logEvent(event: LogEvent) {
        val redactedMessage = PrivacyLogInterceptor.redact(event.message)
        val redactedMetadata = PrivacyLogInterceptor.redactMetadata(event.metadata)
        
        val finalEvent = event.copy(message = redactedMessage, metadata = redactedMetadata)
        val fullMessage = finalEvent.toString()

        when (finalEvent.severity) {
            LogLevel.VERBOSE -> Log.v(finalEvent.category.name, fullMessage, finalEvent.exception)
            LogLevel.DEBUG -> Log.d(finalEvent.category.name, fullMessage, finalEvent.exception)
            LogLevel.INFO -> Log.i(finalEvent.category.name, fullMessage, finalEvent.exception)
            LogLevel.WARNING -> Log.w(finalEvent.category.name, fullMessage, finalEvent.exception)
            LogLevel.ERROR -> Log.e(finalEvent.category.name, fullMessage, finalEvent.exception)
            LogLevel.CRITICAL -> {
                Log.e(finalEvent.category.name, "CRITICAL: $fullMessage", finalEvent.exception)
                crashReporter?.logNonFatal(finalEvent.exception ?: Exception(fullMessage), redactedMetadata)
            }
        }
        
        // Pass to TelemetryStore in Phase 5
        TelemetryStore.record(finalEvent)
    }

    fun log(
        level: LogLevel,
        category: LogCategory,
        message: String,
        metadata: Map<String, String>? = null,
        t: Throwable? = null
    ) {
        val event = LogEvent(
            timestamp = System.currentTimeMillis(),
            category = category,
            severity = level,
            module = "Unknown",
            component = "Legacy",
            sessionId = null,
            peerIdHash = null,
            operationId = TraceManager.currentOperationId,
            threadName = Thread.currentThread().name,
            message = message,
            metadata = metadata,
            exception = t
        )
        logEvent(event)
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
