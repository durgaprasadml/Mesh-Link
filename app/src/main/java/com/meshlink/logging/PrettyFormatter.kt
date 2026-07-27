package com.meshlink.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats log events in a human-readable console output style.
 */
class PrettyFormatter : LogFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun format(event: LogEvent): String {
        val timeString = dateFormat.format(Date(event.timestamp))
        val sb = java.lang.StringBuilder()
        sb.append("[$timeString] ")
        sb.append("[${event.category.name}] ")
        sb.append("[${event.threadName}] ")
        
        // Append Context if present
        if (event.context != LogContext.EMPTY) {
            val ctx = mutableListOf<String>()
            event.context.nodeId?.let { ctx.add("node=$it") }
            event.context.peerId?.let { ctx.add("peer=$it") }
            event.context.packetId?.let { ctx.add("packet=$it") }
            event.context.traceId?.let { ctx.add("trace=$it") }
            event.context.sessionId?.let { ctx.add("session=$it") }
            event.context.route?.let { ctx.add("route=$it") }
            event.context.transport?.let { ctx.add("transport=$it") }
            
            if (ctx.isNotEmpty()) {
                sb.append("{${ctx.joinToString(", ")}} ")
            }
        }
        
        sb.append("- ${event.message}")
        
        // Append Metadata if present
        event.metadata?.let { meta ->
            if (meta.isNotEmpty()) {
                val metaStr = meta.entries.joinToString(", ") { "${it.key}=${it.value}" }
                sb.append(" | metadata=[$metaStr]")
            }
        }
        
        // Append Exception if present
        event.exception?.let { ex ->
            sb.append("\nException: ${ex.javaClass.name}: ${ex.message}")
            ex.stackTrace.take(10).forEach { element ->
                sb.append("\n\tat $element")
            }
            if (ex.stackTrace.size > 10) {
                sb.append("\n\t... ${ex.stackTrace.size - 10} more")
            }
        }
        
        return sb.toString()
    }
}
