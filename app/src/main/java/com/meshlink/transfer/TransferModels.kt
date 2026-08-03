package com.meshlink.transfer

enum class TransferState {
    QUEUED,
    WAITING,
    PREPARING,
    CONNECTING,
    STREAMING,
    COMPRESSING,
    SENDING,
    RECEIVING,
    PAUSED,
    RESUMING,
    RETRYING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class TransferPriority(val value: Int) {
    CRITICAL(5), // SOS
    HIGH(4),     // Voice Notes
    MEDIUM(3),   // Images / Documents
    LOW(2),      // Videos
    BACKGROUND(1) // Sync
}

enum class TransferDirection {
    INCOMING,
    OUTGOING
}

enum class TransportType {
    BLE,
    WIFI_DIRECT,
    HYBRID,
    UNKNOWN
}

data class TransferSession(
    val transferId: String,
    val senderId: String,
    val targetId: String,
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val totalChunks: Int,
    val direction: TransferDirection,
    var state: TransferState = TransferState.QUEUED,
    var priority: TransferPriority = TransferPriority.MEDIUM,
    var transportUsed: TransportType = TransportType.UNKNOWN,
    var bytesTransferred: Long = 0L,
    var chunksTransferred: Int = 0,
    var sha256Checksum: String? = null,
    var startTimeMs: Long = 0L,
    var endTimeMs: Long = 0L,
    var retries: Int = 0,
    var filePath: String? = null, // Null until assembled, or points to source file for outgoing
    var thumbnailBase64: String? = null
) {
    fun getProgress(): Float {
        if (totalChunks <= 0) return 0f
        return (chunksTransferred.toFloat() / totalChunks.toFloat()).coerceIn(0f, 1f)
    }

    fun getAverageSpeedBytesPerSec(): Float {
        val elapsed = (if (endTimeMs > 0) endTimeMs else System.currentTimeMillis()) - startTimeMs
        if (elapsed <= 0) return 0f
        return (bytesTransferred.toFloat() / (elapsed / 1000f))
    }

    fun getRemainingBytes(): Long {
        return (totalBytes - bytesTransferred).coerceAtLeast(0L)
    }

    fun getEstimatedEtaSeconds(): Long {
        val speed = getAverageSpeedBytesPerSec()
        if (speed <= 0f) return -1L
        return (getRemainingBytes() / speed).toLong()
    }

    fun isTerminal(): Boolean = state.isTerminal()

    fun isActive(): Boolean = state.isActive()

    fun requiresCleanup(): Boolean = state.requiresCleanup()
}

fun TransferState.isTerminal(): Boolean {
    return this == TransferState.COMPLETED || this == TransferState.FAILED || this == TransferState.CANCELLED
}

fun TransferState.isActive(): Boolean {
    return !isTerminal()
}

fun TransferState.requiresCleanup(): Boolean {
    return this == TransferState.FAILED || this == TransferState.CANCELLED
}


