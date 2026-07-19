package com.meshlink.common.logger

import java.util.UUID

object TraceManager {
    
    // Simplistic ThreadLocal implementation for managing tracing boundaries
    private val _currentOperationId = ThreadLocal<String>()
    
    val currentOperationId: String?
        get() = _currentOperationId.get()

    fun beginTrace(): String {
        val traceId = UUID.randomUUID().toString()
        _currentOperationId.set(traceId)
        MeshLogger.logEvent(LogEvent(
            timestamp = System.currentTimeMillis(),
            category = LogCategory.SYSTEM,
            severity = LogLevel.INFO,
            module = "TraceManager",
            component = "Lifecycle",
            sessionId = null,
            peerIdHash = null,
            operationId = traceId,
            threadName = Thread.currentThread().name,
            message = "Trace Started"
        ))
        return traceId
    }
    
    fun endTrace(traceId: String, status: String = "SUCCESS", failureReason: String? = null) {
        MeshLogger.logEvent(LogEvent(
            timestamp = System.currentTimeMillis(),
            category = LogCategory.SYSTEM,
            severity = LogLevel.INFO,
            module = "TraceManager",
            component = "Lifecycle",
            sessionId = null,
            peerIdHash = null,
            operationId = traceId,
            threadName = Thread.currentThread().name,
            message = "Trace Ended",
            metadata = mapOf("status" to status, "failureReason" to (failureReason ?: ""))
        ))
        if (_currentOperationId.get() == traceId) {
            _currentOperationId.remove()
        }
    }
    
    inline fun <T> trace(block: () -> T): T {
        val id = beginTrace()
        return try {
            val result = block()
            endTrace(id, "SUCCESS")
            result
        } catch (e: Exception) {
            endTrace(id, "FAILED", e.message)
            throw e
        }
    }
}
