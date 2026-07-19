package com.meshlink.common.metrics

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsManager @Inject constructor() {
    
    private val counters = ConcurrentHashMap<String, Long>()
    private val timers = ConcurrentHashMap<String, MutableList<Long>>()
    private val gauges = ConcurrentHashMap<String, () -> Number>()

    fun incrementCounter(metricName: String, value: Long = 1L) {
        counters[metricName] = (counters[metricName] ?: 0L) + value
    }

    fun recordTimer(metricName: String, durationMs: Long) {
        timers.getOrPut(metricName) { mutableListOf() }.add(durationMs)
    }

    fun getCounter(metricName: String): Long {
        return counters[metricName] ?: 0L
    }

    fun registerGauge(metricName: String, provider: () -> Number) {
        gauges[metricName] = provider
    }

    fun getGauge(metricName: String): Number {
        return gauges[metricName]?.invoke() ?: 0
    }

    fun getAverageTime(metricName: String): Long {
        val list = timers[metricName]
        if (list.isNullOrEmpty()) return 0L
        return list.average().toLong()
    }

    fun getAllMetrics(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Add runtime memory stats
        val runtime = Runtime.getRuntime()
        result["heap_max_mb"] = runtime.maxMemory() / (1024 * 1024)
        result["heap_total_mb"] = runtime.totalMemory() / (1024 * 1024)
        result["heap_free_mb"] = runtime.freeMemory() / (1024 * 1024)
        result["heap_used_mb"] = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        
        counters.forEach { (k, v) -> result[k] = v }
        timers.forEach { (k, list) ->
            if (list.isNotEmpty()) {
                result["${k}_avg_ms"] = list.average().toLong()
                result["${k}_count"] = list.size
            }
        }
        gauges.forEach { (k, provider) ->
            try {
                result[k] = provider()
            } catch (e: Exception) {
                result[k] = -1
            }
        }
        return result
    }
}
