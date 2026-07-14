package com.meshlink.transfer

enum class TransferState {
    WAITING,
    PREPARING,
    COMPRESSING,
    SENDING,
    RECEIVING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED,
    RESUMING
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
    var state: TransferState = TransferState.WAITING,
    var priority: TransferPriority = TransferPriority.MEDIUM,
    var transportUsed: TransportType = TransportType.UNKNOWN,
    var bytesTransferred: Long = 0L,
    var chunksTransferred: Int = 0,
    var sha256Checksum: String? = null,
    var startTimeMs: Long = 0L,
    var endTimeMs: Long = 0L,
    var retries: Int = 0,
    var filePath: String? = null // Null until assembled, or points to source file for outgoing
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
}
