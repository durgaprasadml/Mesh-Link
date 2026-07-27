package com.meshlink.logging

/**
 * Represents a fully constructed, structured log event.
 */
data class LogEvent(
    val timestamp: Long,
    val level: LogLevel,
    val category: LogCategory,
    val message: String,
    val context: LogContext,
    val threadName: String,
    val exception: Throwable? = null,
    val metadata: Map<String, Any>? = null
)
