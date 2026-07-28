package com.meshlink.logging

import org.json.JSONObject

/**
 * Formats log events into a single-line JSON structure for structured ingestion.
 */
class JsonFormatter : LogFormatter {
    override fun format(event: LogEvent): String {
        val json = JSONObject()
        json.put("timestamp", event.timestamp)
        json.put("level", event.level.name)
        json.put("category", event.category.name)
        json.put("message", event.message)
        json.put("thread", event.threadName)

        if (event.context != LogContext.EMPTY) {
            val ctxJson = JSONObject()
            event.context.nodeId?.let { ctxJson.put("nodeId", it) }
            event.context.peerId?.let { ctxJson.put("peerId", it) }
            event.context.packetId?.let { ctxJson.put("packetId", it) }
            event.context.traceId?.let { ctxJson.put("traceId", it) }
            event.context.sessionId?.let { ctxJson.put("sessionId", it) }
            event.context.route?.let { ctxJson.put("route", it) }
            event.context.transport?.let { ctxJson.put("transport", it) }
            if (ctxJson.length() > 0) {
                json.put("context", ctxJson)
            }
        }

        event.metadata?.let { meta ->
            if (meta.isNotEmpty()) {
                val metaJson = JSONObject()
                meta.forEach { (k, v) ->
                    // Convert basic types, let JSONObject handle the rest
                    metaJson.put(k, v)
                }
                json.put("metadata", metaJson)
            }
        }

        event.exception?.let { ex ->
            val exJson = JSONObject()
            exJson.put("type", ex.javaClass.name)
            exJson.put("message", ex.message)
            // Limit stacktrace in JSON to avoid massive payloads
            val trace = ex.stackTrace.take(15).joinToString("\n") { it.toString() }
            exJson.put("stackTrace", trace)
            json.put("exception", exJson)
        }

        return json.toString()
    }
}
