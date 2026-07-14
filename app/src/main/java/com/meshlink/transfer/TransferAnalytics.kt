package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferAnalytics @Inject constructor() {
    companion object {
        private const val TAG = "TransferAnalytics"
    }

    fun recordTransferStarted(session: TransferSession) {
        MeshLogger.d(TAG, "Started transfer ${session.transferId} via ${session.transportUsed} (${session.totalBytes} bytes)")
    }

    fun recordTransferCompleted(session: TransferSession) {
        val speedStr = String.format("%.2f KB/s", session.getAverageSpeedBytesPerSec() / 1024f)
        val elapsed = session.endTimeMs - session.startTimeMs
        MeshLogger.d(TAG, "Completed transfer ${session.transferId} in ${elapsed}ms. Speed: $speedStr. Retries: ${session.retries}")
    }

    fun recordTransferFailed(session: TransferSession, reason: String) {
        MeshLogger.e(TAG, "Transfer ${session.transferId} failed: $reason")
    }
    
    fun recordChunkRetransmission(transferId: String, chunkIndex: Int) {
        MeshLogger.w(TAG, "Retransmitting chunk $chunkIndex for $transferId")
    }
}
