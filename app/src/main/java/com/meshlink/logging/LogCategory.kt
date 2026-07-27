package com.meshlink.logging

/**
 * Represents the functional area generating a log event.
 * Every log entry must belong to exactly one category.
 */
enum class LogCategory {
    BLE,
    ROUTING,
    TRANSPORT,
    SECURITY,
    SESSION,
    MESSAGING,
    MEDIA,
    VOICE,
    STORE_FORWARD,
    PERFORMANCE,
    SYSTEM
}
