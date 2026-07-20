package com.meshlink.security.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SecurityLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val description: String,
    val severity: Severity
) {
    enum class Severity { INFO, WARNING, ERROR }
    
    fun formattedTime(): String {
        return SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }
}

@Singleton
class SecurityLogManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val maxLogs = 500
    private val logs = ConcurrentLinkedDeque<SecurityLogEntry>()
    private val _logsFlow = MutableStateFlow<List<SecurityLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<SecurityLogEntry>> = _logsFlow.asStateFlow()

    fun log(eventType: String, description: String, severity: SecurityLogEntry.Severity = SecurityLogEntry.Severity.INFO) {
        val entry = SecurityLogEntry(eventType = eventType, description = description, severity = severity)
        logs.addFirst(entry)
        if (logs.size > maxLogs) {
            logs.removeLast()
        }
        _logsFlow.value = logs.toList()
    }

    fun exportLogs(): File? {
        return try {
            val file = File(context.cacheDir, "mesh_security_logs_${System.currentTimeMillis()}.txt")
            file.printWriter().use { out ->
                out.println("--- MESH LINK SECURITY LOGS ---")
                out.println("Exported at: ${Date()}")
                out.println()
                logs.forEach { entry ->
                    out.println("[${entry.formattedTime()}] [${entry.severity.name}] ${entry.eventType}: ${entry.description}")
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
    
    fun clearLogs() {
        logs.clear()
        _logsFlow.value = emptyList()
    }
}
