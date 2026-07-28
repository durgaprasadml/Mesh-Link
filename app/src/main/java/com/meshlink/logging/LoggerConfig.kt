package com.meshlink.logging

/**
 * Configuration for the MeshLogger.
 */
data class LoggerConfig(
    val minLevel: LogLevel = LogLevel.DEBUG,
    val enabledCategories: Set<LogCategory> = LogCategory.values().toSet(),
    val isEnabled: Boolean = true
)
