package com.meshlink.common.logger

data class LogEvent(
    val timestamp: Long,
    val category: LogCategory,
    val severity: LogLevel,
    val module: String,
    val component: String,
    val sessionId: String?,
    val peerIdHash: String?,
    val operationId: String?,
    val threadName: String,
    val message: String,
    val metadata: Map<String, String>? = null,
    val exception: Throwable? = null
) {
    override fun toString(): String {
        val metaString = if (!metadata.isNullOrEmpty()) " | Meta: $metadata" else ""
        val opString = if (operationId != null) " [Op:$operationId]" else ""
        return "[$category][$severity][$module::$component][T:$threadName]$opString $message$metaString"
    }
}
