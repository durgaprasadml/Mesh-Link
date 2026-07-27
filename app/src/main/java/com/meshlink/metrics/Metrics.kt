package com.meshlink.metrics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Base interface for all metrics.
 *
 * Lifecycle:
 * - Creation: Metrics are typically created lazily via [MetricsRegistry] or [MetricsCollector].
 * - Update: Metrics are updated concurrently from multiple threads.
 * - Reset: Some metrics can be reset (e.g., cleared counters or histograms).
 * - Snapshot: Metrics are periodically snapshot for export.
 */
interface Metric {
    val name: String
    val category: MetricCategory
    val labels: Map<String, String>

    /**
     * Resets the metric to its initial state if applicable.
     */
    fun reset()
}

/**
 * A metric that represents a monotonically increasing value.
 */
class CounterMetric(
    override val name: String,
    override val category: MetricCategory,
    override val labels: Map<String, String> = emptyMap()
) : Metric {
    private val value = AtomicLong(0)

    fun increment(amount: Long = 1) {
        if (amount > 0) {
            value.addAndGet(amount)
        }
    }

    fun get(): Long = value.get()

    override fun reset() {
        value.set(0)
    }
}

/**
 * A metric that represents a single numerical value that can arbitrarily go up and down.
 * It can be backed by a provider lambda or an explicitly set value.
 */
class GaugeMetric(
    override val name: String,
    override val category: MetricCategory,
    override val labels: Map<String, String> = emptyMap(),
    private val provider: (() -> Long)? = null
) : Metric {
    private val explicitValue = AtomicLong(0)

    fun set(value: Long) {
        explicitValue.set(value)
    }

    fun get(): Long {
        return provider?.invoke() ?: explicitValue.get()
    }

    override fun reset() {
        explicitValue.set(0)
    }
}

/**
 * A metric that tracks the distribution of events, such as latencies or sizes.
 */
class HistogramMetric(
    override val name: String,
    override val category: MetricCategory,
    override val labels: Map<String, String> = emptyMap(),
    private val buckets: LongArray
) : Metric {
    private val count = AtomicLong(0)
    private val sum = AtomicLong(0)

    private val bucketCounts = ConcurrentHashMap<Long, AtomicLong>()

    init {
        buckets.forEach { bucketCounts[it] = AtomicLong(0) }
        // For values larger than the biggest bucket
        bucketCounts[Long.MAX_VALUE] = AtomicLong(0)
    }

    fun record(value: Long) {
        count.incrementAndGet()
        sum.addAndGet(value)

        var bucketFound = false
        for (bucket in buckets) {
            if (value <= bucket) {
                bucketCounts[bucket]?.incrementAndGet()
                bucketFound = true
                break
            }
        }
        if (!bucketFound) {
            bucketCounts[Long.MAX_VALUE]?.incrementAndGet()
        }
    }

    fun getCount(): Long = count.get()
    fun getSum(): Long = sum.get()

    fun getSnapshot(): Map<Long, Long> {
        val snapshot = mutableMapOf<Long, Long>()
        bucketCounts.forEach { (bucket, atomicCount) ->
            snapshot[bucket] = atomicCount.get()
        }
        return snapshot
    }

    override fun reset() {
        count.set(0)
        sum.set(0)
        bucketCounts.values.forEach { it.set(0) }
    }
}

/**
 * A specialized metric for measuring durations, backed by a Histogram.
 */
class TimerMetric(
    override val name: String,
    override val category: MetricCategory,
    override val labels: Map<String, String> = emptyMap(),
    buckets: LongArray
) : Metric {

    private val histogram = HistogramMetric(name, category, labels, buckets)

    fun recordDuration(durationMs: Long) {
        histogram.record(durationMs)
    }

    /**
     * Executes a block and records its duration in milliseconds.
     */
    inline fun <T> time(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block()
        } finally {
            val duration = System.currentTimeMillis() - start
            recordDuration(duration)
        }
    }

    fun getCount(): Long = histogram.getCount()
    fun getSum(): Long = histogram.getSum()
    fun getSnapshot(): Map<Long, Long> = histogram.getSnapshot()

    override fun reset() {
        histogram.reset()
    }
}
