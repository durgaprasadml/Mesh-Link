package com.meshlink.logging

/**
 * Represents the severity level of a log event.
 */
enum class LogLevel(val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6)
}
