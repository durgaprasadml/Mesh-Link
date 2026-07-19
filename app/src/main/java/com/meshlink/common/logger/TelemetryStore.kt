package com.meshlink.common.logger

import android.content.Context
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

object TelemetryStore {
    
    // An enterprise would use an encrypted rolling SQLite DB or file buffer here.
    // For this simulation, we use a thread-safe queue acting as a circular buffer.
    private const val MAX_LOG_RETENTION = 5000
    private val memoryBuffer = ConcurrentLinkedQueue<LogEvent>()

    fun record(event: LogEvent) {
        if (memoryBuffer.size >= MAX_LOG_RETENTION) {
            memoryBuffer.poll() // Remove oldest event
        }
        memoryBuffer.offer(event)
    }

    fun exportToDisk(context: Context): File? {
        // Mocking a disk export of telemetry data
        try {
            val telemetryDir = File(context.cacheDir, "telemetry")
            if (!telemetryDir.exists()) telemetryDir.mkdirs()
            
            val dumpFile = File(telemetryDir, "telemetry_dump_${System.currentTimeMillis()}.json")
            
            // In a real scenario, serialize memoryBuffer to JSON here and write to dumpFile
            // using compression and encryption.
            dumpFile.writeText("{\"events\": ${memoryBuffer.size}}") 
            
            return dumpFile
        } catch (e: Exception) {
            MeshLogger.e("TelemetryStore", "Failed to export telemetry", e)
            return null
        }
    }
    
    fun getRecentLogs(limit: Int = 100): List<LogEvent> {
        return memoryBuffer.toList().takeLast(limit)
    }
    
    fun clear() {
        memoryBuffer.clear()
    }
}
