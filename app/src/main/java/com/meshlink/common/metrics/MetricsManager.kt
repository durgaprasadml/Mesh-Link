package com.meshlink.common.metrics

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsManager @Inject constructor() {
    
    private val counters = ConcurrentHashMap<String, Long>()
    private val timers = ConcurrentHashMap<String, MutableList<Long>>()

    fun incrementCounter(metricName: String, value: Long = 1L) {
        counters[metricName] = (counters[metricName] ?: 0L) + value
    }

    fun recordTimer(metricName: String, durationMs: Long) {
        timers.getOrPut(metricName) { mutableListOf() }.add(durationMs)
    }

    fun getCounter(metricName: String): Long {
        return counters[metricName] ?: 0L
    }

    fun getAverageTime(metricName: String): Long {
        val list = timers[metricName]
        if (list.isNullOrEmpty()) return 0L
        return list.average().toLong()
    }

    fun getAllMetrics(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        counters.forEach { (k, v) -> result[k] = v }
        timers.forEach { (k, list) ->
            if (list.isNotEmpty()) {
                result["${k}_avg_ms"] = list.average().toLong()
                result["${k}_count"] = list.size
            }
        }
        return result
    }
}
