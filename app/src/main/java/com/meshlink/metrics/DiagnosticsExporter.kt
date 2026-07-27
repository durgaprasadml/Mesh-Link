package com.meshlink.metrics

import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports [DiagnosticsSnapshot] instances to different formats.
 */
class DiagnosticsExporter {

    fun exportToText(snapshot: DiagnosticsSnapshot): String {
        val sb = java.lang.StringBuilder()
        sb.append("=== Mesh Link Diagnostics Snapshot ===\n")
        sb.append("Timestamp: ${snapshot.timestamp}\n")
        sb.append("App Version: ${snapshot.appVersion}\n")
        sb.append("Runtime Duration: ${snapshot.runtimeDurationMs} ms\n")
        sb.append("Enabled Categories: ${snapshot.enabledCategories.joinToString(", ")}\n")
        sb.append("--------------------------------------\n")

        snapshot.metrics.groupBy { it.category }.forEach { (category, categoryMetrics) ->
            sb.append("Category: $category\n")
            categoryMetrics.sortedBy { it.name }.forEach { metric ->
                sb.append("  [${metric.type}] ${metric.name}")
                if (metric.labels.isNotEmpty()) {
                    sb.append("{${metric.labels.entries.joinToString(",") { "${it.key}=${it.value}" }}}")
                }
                when (metric.type) {
                    "counter", "gauge" -> sb.append(": ${metric.value}\n")
                    "histogram", "timer" -> {
                        sb.append(":\n    count=${metric.count}, sum=${metric.sum}\n")
                        metric.histogram?.toSortedMap()?.forEach { (bucket, count) ->
                            val bucketStr = if (bucket == Long.MAX_VALUE) "+Inf" else bucket.toString()
                            sb.append("    le_\"$bucketStr\" = $count\n")
                        }
                    }
                }
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    fun exportToJson(snapshot: DiagnosticsSnapshot): String {
        val root = JSONObject()
        root.put("timestamp", snapshot.timestamp)
        root.put("appVersion", snapshot.appVersion)
        root.put("runtimeDurationMs", snapshot.runtimeDurationMs)
        root.put("enabledCategories", JSONArray(snapshot.enabledCategories.map { it.name }))

        val metricsArray = JSONArray()
        snapshot.metrics.forEach { metric ->
            val mObj = JSONObject()
            mObj.put("name", metric.name)
            mObj.put("category", metric.category.name)
            mObj.put("type", metric.type)

            if (metric.labels.isNotEmpty()) {
                val labelsObj = JSONObject()
                metric.labels.forEach { (k, v) -> labelsObj.put(k, v) }
                mObj.put("labels", labelsObj)
            }

            metric.value?.let { mObj.put("value", it) }
            metric.count?.let { mObj.put("count", it) }
            metric.sum?.let { mObj.put("sum", it) }

            metric.histogram?.let { hist ->
                val histObj = JSONObject()
                hist.forEach { (k, v) ->
                    val key = if (k == Long.MAX_VALUE) "+Inf" else k.toString()
                    histObj.put(key, v)
                }
                mObj.put("histogram", histObj)
            }

            metricsArray.put(mObj)
        }

        root.put("metrics", metricsArray)
        return root.toString(2)
    }
}
