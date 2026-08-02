package com.meshlink.ui.media.models

import androidx.compose.runtime.Immutable

/**
 * UI presentation models for active file transfers and telemetry statistics.
 */
enum class TransferStatus {
    QUEUED,
    PREPARING,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class TransferDirectionUi {
    INCOMING,
    OUTGOING
}

@Immutable
data class TransferUi(
    val transferId: String,
    val fileName: String,
    val direction: TransferDirectionUi,
    val progress: Float, // 0f..1f
    val speedBytesPerSec: Float,
    val totalSizeBytes: Long,
    val transferredSizeBytes: Long,
    val status: TransferStatus,
    val transportType: String = "HYBRID", // BLE, WIFI_DIRECT, HYBRID
    val retryCount: Int = 0,
    val crcErrors: Int = 0,
    val priorityName: String = "MEDIUM",
    val mimeType: String = "application/octet-stream"
) {
    val speedFormatted: String
        get() {
            return when {
                speedBytesPerSec >= 1_000_000 -> String.format("%.2f MB/s", speedBytesPerSec / 1_000_000f)
                speedBytesPerSec >= 1_000 -> String.format("%.1f KB/s", speedBytesPerSec / 1_000f)
                else -> String.format("%.0f B/s", speedBytesPerSec)
            }
        }

    val etaFormatted: String
        get() {
            if (speedBytesPerSec <= 0 || progress >= 1f) return "Calculating..."
            val remainingBytes = totalSizeBytes - transferredSizeBytes
            if (remainingBytes <= 0) return "Finishing..."
            val secondsLeft = (remainingBytes / speedBytesPerSec).toLong()
            return when {
                secondsLeft >= 3600 -> "${secondsLeft / 3600}h ${(secondsLeft % 3600) / 60}m"
                secondsLeft >= 60 -> "${secondsLeft / 60}m ${secondsLeft % 60}s"
                else -> "${secondsLeft}s"
            }
        }
}

@Immutable
data class TransferStatisticsUi(
    val filesSentCount: Int = 0,
    val filesReceivedCount: Int = 0,
    val totalTransferredBytes: Long = 0L,
    val failedTransfersCount: Int = 0,
    val averageSpeedBytesPerSec: Float = 0f,
    val bleTransfersCount: Int = 0,
    val wifiTransfersCount: Int = 0,
    val activeSlidingWindowSize: Int = 4,
    val transportModeName: String = "HYBRID_ACTIVE"
) {
    val totalVolumeFormatted: String
        get() {
            return when {
                totalTransferredBytes >= 1_073_741_824 -> String.format("%.2f GB", totalTransferredBytes / 1_073_741_824f)
                totalTransferredBytes >= 1_048_576 -> String.format("%.1f MB", totalTransferredBytes / 1_048_576f)
                totalTransferredBytes >= 1024 -> String.format("%.1f KB", totalTransferredBytes / 1024f)
                else -> "$totalTransferredBytes B"
            }
        }

    val successRatePercent: Int
        get() {
            val total = filesSentCount + filesReceivedCount + failedTransfersCount
            if (total == 0) return 100
            val successful = filesSentCount + filesReceivedCount
            return ((successful.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
        }
}
