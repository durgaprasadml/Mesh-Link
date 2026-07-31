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
    CRITICAL(5),  // SOS / Emergency Media
    HIGH(4),      // Voice Notes
    MEDIUM(3),    // Images / Documents
    LOW(2),       // Video Files
    BACKGROUND(1) // Sync / Backup
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
    var startTimeMs: Long = System.currentTimeMillis(),
    var endTimeMs: Long = 0L,
    var lastUpdatedMs: Long = System.currentTimeMillis(),
    var retries: Int = 0,
    var crc32Errors: Int = 0,
    var resumeCount: Int = 0,
    var packetLossRate: Float = 0f,
    var averageRttMs: Long = 100L,
    var slidingWindowSize: Int = 4,
    var lastConfirmedChunkIndex: Int = -1,
    var compressionType: String = "NONE",
    var compressedSize: Long = totalBytes,
    var filePath: String? = null // Points to source file for outgoing or local target file when incoming completed
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

    val compressionRatio: Float
        get() = if (totalBytes > 0) compressedSize.toFloat() / totalBytes.toFloat() else 1.0f
}
