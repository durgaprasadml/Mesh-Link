package com.meshlink.metrics

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MetricsTest {

    private lateinit var registry: MetricsRegistry
    private lateinit var config: MetricsConfig
    private lateinit var collector: MetricsCollector

    @Before
    fun setup() {
        registry = MetricsRegistry()
        config = MetricsConfig(globalSamplingRate = 1.0)
        collector = MetricsCollector(registry, config)
    }

    @Test
    fun testCounterIncrement() {
        collector.incrementCounter("test.counter", MetricCategory.TRANSPORT, emptyMap(), 5)
        collector.incrementCounter("test.counter", MetricCategory.TRANSPORT, emptyMap(), 10)

        val counter = registry.counter("test.counter", MetricCategory.TRANSPORT)
        assertEquals(15, counter.get())
    }

    @Test
    fun testCounterConcurrency() {
        val counterName = "concurrent.counter"
        val threads = 10
        val incrementsPerThread = 1000
        val latch = CountDownLatch(threads)
        val executor = Executors.newFixedThreadPool(threads)

        for (i in 0 until threads) {
            executor.submit {
                for (j in 0 until incrementsPerThread) {
                    collector.incrementCounter(counterName, MetricCategory.SYSTEM, emptyMap())
                }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        val counter = registry.counter(counterName, MetricCategory.SYSTEM)
        assertEquals((threads * incrementsPerThread).toLong(), counter.get())
    }

    @Test
    fun testHistogramBucketing() {
        val buckets = longArrayOf(10, 50, 100)
        collector.recordHistogram("test.hist", MetricCategory.PERFORMANCE, emptyMap(), 5, buckets)
        collector.recordHistogram("test.hist", MetricCategory.PERFORMANCE, emptyMap(), 25, buckets)
        collector.recordHistogram("test.hist", MetricCategory.PERFORMANCE, emptyMap(), 75, buckets)
        collector.recordHistogram("test.hist", MetricCategory.PERFORMANCE, emptyMap(), 150, buckets)

        val hist = registry.histogram("test.hist", MetricCategory.PERFORMANCE, emptyMap(), buckets)
        val snapshot = hist.getSnapshot()

        assertEquals(4L, hist.getCount())
        assertEquals(255L, hist.getSum())

        assertEquals(1L, snapshot[10L])
        assertEquals(1L, snapshot[50L])
        assertEquals(1L, snapshot[100L])
        assertEquals(1L, snapshot[Long.MAX_VALUE]) // The 150 value goes here
    }

    @Test
    fun testSnapshotImmutabilityAndFactory() {
        collector.incrementCounter("snapshot.counter", MetricCategory.MESSAGING, emptyMap(), 42)
        
        val factory = DiagnosticsSnapshotFactory("1.0.0", System.currentTimeMillis() - 1000)
        val snapshot = factory.create(registry, config)

        assertEquals("1.0.0", snapshot.appVersion)
        assertTrue(snapshot.runtimeDurationMs >= 1000)

        val counterData = snapshot.metrics.find { it.name == "snapshot.counter" }
        assertNotNull(counterData)
        assertEquals(42L, counterData?.value)

        // Modify registry after snapshot
        collector.incrementCounter("snapshot.counter", MetricCategory.MESSAGING, emptyMap(), 10)

        // Snapshot should remain unchanged
        assertEquals(42L, counterData?.value)
    }

    @Test
    fun testDiagnosticsExporterJSON() {
        collector.incrementCounter("export.counter", MetricCategory.ROUTING, mapOf("type" to "multi-hop"), 5)
        
        val factory = DiagnosticsSnapshotFactory("1.0.0", System.currentTimeMillis())
        val snapshot = factory.create(registry, config)
        
        val exporter = DiagnosticsExporter()
        val jsonString = exporter.exportToJson(snapshot)
        
        val root = JSONObject(jsonString)
        assertEquals("1.0.0", root.getString("appVersion"))
        
        val metricsArray = root.getJSONArray("metrics")
        assertTrue(metricsArray.length() > 0)
        
        val firstMetric = metricsArray.getJSONObject(0)
        assertEquals("export.counter", firstMetric.getString("name"))
        assertEquals("ROUTING", firstMetric.getString("category"))
        assertEquals(5L, firstMetric.getLong("value"))
        
        val labels = firstMetric.getJSONObject("labels")
        assertEquals("multi-hop", labels.getString("type"))
    }
    
    @Test
    fun testSampling() {
        val lowSamplingConfig = MetricsConfig(globalSamplingRate = 0.0) // 0%
        val lowSamplingCollector = MetricsCollector(registry, lowSamplingConfig)
        
        lowSamplingCollector.recordTimer("test.timer", MetricCategory.TRANSPORT, emptyMap(), 100)
        
        val timer = registry.timer("test.timer", MetricCategory.TRANSPORT, emptyMap(), longArrayOf(10))
        // Because sampling rate is 0, the timer should not be recorded
        assertEquals(0L, timer.getCount())
    }
}
