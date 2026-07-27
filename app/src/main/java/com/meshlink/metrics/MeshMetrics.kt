package com.meshlink.metrics

/**
 * Singleton facade for interacting with the metrics framework.
 * This is the primary entry point for the application to record metrics.
 */
object MeshMetrics {

    private lateinit var registry: MetricsRegistry
    private lateinit var config: MetricsConfig
    private lateinit var collector: MetricsCollector
    private lateinit var snapshotFactory: DiagnosticsSnapshotFactory
    private lateinit var exporter: DiagnosticsExporter

    private var initialized = false

    /**
     * Initializes the metrics framework.
     */
    @Synchronized
    fun initialize(
        appVersion: String,
        configuration: MetricsConfig = MetricsConfig()
    ) {
        if (initialized) return

        registry = MetricsRegistry()
        config = configuration
        collector = MetricsCollector(registry, config)
        snapshotFactory = DiagnosticsSnapshotFactory(appVersion, System.currentTimeMillis())
        exporter = DiagnosticsExporter()

        initialized = true
    }

    /**
     * Increments a counter.
     */
    fun incrementCounter(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        amount: Long = 1
    ) {
        if (!initialized) return
        collector.incrementCounter(name, category, labels, amount)
    }

    /**
     * Updates a gauge explicitly.
     */
    fun updateGauge(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        value: Long
    ) {
        if (!initialized) return
        collector.updateGauge(name, category, labels, value)
    }

    /**
     * Records a duration in a timer metric.
     */
    fun recordTimer(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        durationMs: Long,
        buckets: LongArray? = null
    ) {
        if (!initialized) return
        collector.recordTimer(name, category, labels, durationMs, buckets)
    }

    /**
     * Records a value in a histogram.
     */
    fun recordHistogram(
        name: String,
        category: MetricCategory,
        labels: Map<String, String> = emptyMap(),
        value: Long,
        buckets: LongArray? = null
    ) {
        if (!initialized) return
        collector.recordHistogram(name, category, labels, value, buckets)
    }

    /**
     * Takes a snapshot of the current metrics.
     */
    fun takeSnapshot(): DiagnosticsSnapshot? {
        if (!initialized) return null
        return snapshotFactory.create(registry, config)
    }

    /**
     * Exports the current metrics snapshot to human-readable text.
     */
    fun exportToText(): String {
        if (!initialized) return "Metrics framework not initialized."
        return exporter.exportToText(snapshotFactory.create(registry, config))
    }

    /**
     * Exports the current metrics snapshot to JSON.
     */
    fun exportToJson(): String {
        if (!initialized) return "{}"
        return exporter.exportToJson(snapshotFactory.create(registry, config))
    }

    /**
     * Clears all metrics in the registry.
     */
    fun clear() {
        if (!initialized) return
        registry.clear()
    }
}
