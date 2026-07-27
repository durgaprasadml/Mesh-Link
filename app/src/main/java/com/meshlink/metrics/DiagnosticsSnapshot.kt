package com.meshlink.metrics

/**
 * An immutable snapshot of the current state of metrics.
 *
 * @property timestamp Epoch time of the snapshot creation.
 * @property appVersion The version of the application.
 * @property runtimeDurationMs How long the application has been running since metrics started.
 * @property enabledCategories Categories that are currently enabled for collection.
 * @property metrics List of serialized metric states.
 */
data class DiagnosticsSnapshot(
    val timestamp: Long,
    val appVersion: String,
    val runtimeDurationMs: Long,
    val enabledCategories: Set<MetricCategory>,
    val metrics: List<MetricData>
) {

    data class MetricData(
        val name: String,
        val category: MetricCategory,
        val type: String,
        val labels: Map<String, String>,
        val value: Long? = null,
        val count: Long? = null,
        val sum: Long? = null,
        val histogram: Map<Long, Long>? = null
    )
}

/**
 * Centralized factory to construct immutable [DiagnosticsSnapshot] instances.
 */
class DiagnosticsSnapshotFactory(
    private val appVersion: String,
    private val startTimeMs: Long
) {

    fun create(
        registry: MetricsRegistry,
        config: MetricsConfig
    ): DiagnosticsSnapshot {
        val allMetrics = registry.getAllMetrics()

        val metricDataList = allMetrics.mapNotNull { metric ->
            when (metric) {
                is CounterMetric -> DiagnosticsSnapshot.MetricData(
                    name = metric.name,
                    category = metric.category,
                    type = "counter",
                    labels = metric.labels,
                    value = metric.get()
                )
                is GaugeMetric -> DiagnosticsSnapshot.MetricData(
                    name = metric.name,
                    category = metric.category,
                    type = "gauge",
                    labels = metric.labels,
                    value = metric.get()
                )
                is HistogramMetric -> DiagnosticsSnapshot.MetricData(
                    name = metric.name,
                    category = metric.category,
                    type = "histogram",
                    labels = metric.labels,
                    count = metric.getCount(),
                    sum = metric.getSum(),
                    histogram = metric.getSnapshot()
                )
                is TimerMetric -> DiagnosticsSnapshot.MetricData(
                    name = metric.name,
                    category = metric.category,
                    type = "timer",
                    labels = metric.labels,
                    count = metric.getCount(),
                    sum = metric.getSum(),
                    histogram = metric.getSnapshot()
                )
                else -> null
            }
        }

        return DiagnosticsSnapshot(
            timestamp = System.currentTimeMillis(),
            appVersion = appVersion,
            runtimeDurationMs = System.currentTimeMillis() - startTimeMs,
            enabledCategories = config.enabledCategories,
            metrics = metricDataList
        )
    }
}
