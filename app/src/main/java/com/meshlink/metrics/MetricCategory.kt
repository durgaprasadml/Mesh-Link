package com.meshlink.metrics

/**
 * Defines the categories of metrics collected by the Mesh Link framework.
 * Used for filtering and grouping metrics in snapshots and exports.
 */
enum class MetricCategory {
    ROUTING,
    TRANSPORT,
    MESSAGING,
    SECURITY,
    SESSION,
    BLE,
    STORE_FORWARD,
    MEDIA,
    VOICE,
    PERFORMANCE,
    SYSTEM
}
