package com.meshlink.metrics

/**
 * Configuration options for the metrics framework.
 *
 * @property enabledCategories The set of categories that are actively collected.
 * @property collectionIntervalMs How often metrics are sampled internally (if applicable).
 * @property snapshotFrequencyMs How often snapshots are automatically taken.
 * @property defaultHistogramBuckets The default bucket boundaries for histograms.
 * @property globalSamplingRate A rate between 0.0 and 1.0 (e.g., 0.1 for 10% sampling) applied to high-frequency metrics.
 */
data class MetricsConfig(
    val enabledCategories: Set<MetricCategory> = MetricCategory.values().toSet(),
    val collectionIntervalMs: Long = 10_000L,
    val snapshotFrequencyMs: Long = 60_000L,
    val defaultHistogramBuckets: LongArray = longArrayOf(1, 5, 10, 25, 50, 100, 250, 500, 1000, 5000),
    val globalSamplingRate: Double = 1.0
) {
    init {
        require(globalSamplingRate in 0.0..1.0) { "globalSamplingRate must be between 0.0 and 1.0" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetricsConfig

        if (enabledCategories != other.enabledCategories) return false
        if (collectionIntervalMs != other.collectionIntervalMs) return false
        if (snapshotFrequencyMs != other.snapshotFrequencyMs) return false
        if (!defaultHistogramBuckets.contentEquals(other.defaultHistogramBuckets)) return false
        if (globalSamplingRate != other.globalSamplingRate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabledCategories.hashCode()
        result = 31 * result + collectionIntervalMs.hashCode()
        result = 31 * result + snapshotFrequencyMs.hashCode()
        result = 31 * result + defaultHistogramBuckets.contentHashCode()
        result = 31 * result + globalSamplingRate.hashCode()
        return result
    }
}
