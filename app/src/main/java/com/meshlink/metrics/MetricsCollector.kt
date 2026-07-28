package com.meshlink.metrics

import kotlin.random.Random

/**
 * Acts as an intermediary between the facade ([MeshMetrics]) and the storage ([MetricsRegistry]).
 * Handles configuration-based filtering and sampling to minimize overhead.
 */
class MetricsCollector(
    private val registry: MetricsRegistry,
    private val config: MetricsConfig
) {

    private val random = Random(System.nanoTime())

    fun incrementCounter(
        name: String,
        category: MetricCategory,
        labels: Map<String, String>,
        amount: Long = 1
    ) {
        if (!shouldCollect(category)) return
        registry.counter(name, category, labels).increment(amount)
    }

    fun updateGauge(
        name: String,
        category: MetricCategory,
        labels: Map<String, String>,
        value: Long
    ) {
        if (!shouldCollect(category)) return
        registry.gauge(name, category, labels).set(value)
    }

    fun recordTimer(
        name: String,
        category: MetricCategory,
        labels: Map<String, String>,
        durationMs: Long,
        buckets: LongArray? = null
    ) {
        if (!shouldCollect(category)) return
        if (!shouldSample()) return

        val actualBuckets = buckets ?: config.defaultHistogramBuckets
        registry.timer(name, category, labels, actualBuckets).recordDuration(durationMs)
    }

    fun recordHistogram(
        name: String,
        category: MetricCategory,
        labels: Map<String, String>,
        value: Long,
        buckets: LongArray? = null
    ) {
        if (!shouldCollect(category)) return
        if (!shouldSample()) return

        val actualBuckets = buckets ?: config.defaultHistogramBuckets
        registry.histogram(name, category, labels, actualBuckets).record(value)
    }

    private fun shouldCollect(category: MetricCategory): Boolean {
        return config.enabledCategories.contains(category)
    }

    private fun shouldSample(): Boolean {
        if (config.globalSamplingRate >= 1.0) return true
        if (config.globalSamplingRate <= 0.0) return false
        return random.nextDouble() <= config.globalSamplingRate
    }
}
