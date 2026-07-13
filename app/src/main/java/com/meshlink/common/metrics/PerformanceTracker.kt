package com.meshlink.common.metrics

class PerformanceTracker(
    private val metricsManager: MetricsManager,
    private val metricName: String
) {
    private val startTime = System.currentTimeMillis()

    fun stop() {
        val duration = System.currentTimeMillis() - startTime
        metricsManager.recordTimer(metricName, duration)
    }
}

inline fun <T> MetricsManager.trackExecution(metricName: String, block: () -> T): T {
    val tracker = PerformanceTracker(this, metricName)
    try {
        return block()
    } finally {
        tracker.stop()
    }
}
