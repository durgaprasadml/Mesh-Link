package com.meshlink.metrics

import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe registry to store and retrieve metrics.
 */
class MetricsRegistry {

    private val metrics = ConcurrentHashMap<String, Metric>()

    /**
     * Gets or creates a [CounterMetric].
     */
    fun counter(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap()
    ): CounterMetric {
        val key = generateKey(name, labels)
        return metrics.computeIfAbsent(key) {
            CounterMetric(name, category, labels)
        } as CounterMetric
    }

    /**
     * Gets or creates a [GaugeMetric].
     */
    fun gauge(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        provider: (() -> Long)? = null
    ): GaugeMetric {
        val key = generateKey(name, labels)
        return metrics.computeIfAbsent(key) {
            GaugeMetric(name, category, labels, provider)
        } as GaugeMetric
    }

    /**
     * Gets or creates a [TimerMetric].
     */
    fun timer(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        buckets: LongArray
    ): TimerMetric {
        val key = generateKey(name, labels)
        return metrics.computeIfAbsent(key) {
            TimerMetric(name, category, labels, buckets)
        } as TimerMetric
    }

    /**
     * Gets or creates a [HistogramMetric].
     */
    fun histogram(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        buckets: LongArray
    ): HistogramMetric {
        val key = generateKey(name, labels)
        return metrics.computeIfAbsent(key) {
            HistogramMetric(name, category, labels, buckets)
        } as HistogramMetric
    }

    /**
     * Retrieves all registered metrics.
     */
    fun getAllMetrics(): List<Metric> {
        return metrics.values.toList()
    }

    /**
     * Clears all metrics from the registry.
     */
    fun clear() {
        metrics.clear()
    }

    private fun generateKey(name: String, labels: Map<String, String>): String {
        if (labels.isEmpty()) return name
        val sortedLabels = labels.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }
        return "$name[$sortedLabels]"
    }
}
