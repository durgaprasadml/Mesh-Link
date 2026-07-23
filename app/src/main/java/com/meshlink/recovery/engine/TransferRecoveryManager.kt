package com.meshlink.recovery.engine

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class TransferState(
    val transferId: String,
    val targetFile: File,
    val totalSize: Long,
    val bytesReceived: Long,
    val isComplete: Boolean
)

@Singleton
class TransferRecoveryManager @Inject constructor() {

    // Simulates an active tracker for chunked file transfers (images, voice notes)
    private val activeTransfers = ConcurrentHashMap<String, TransferState>()

    fun registerTransfer(transferId: String, targetFile: File, totalSize: Long) {
        val existingSize = if (targetFile.exists()) targetFile.length() else 0L
        activeTransfers[transferId] = TransferState(
            transferId = transferId,
            targetFile = targetFile,
            totalSize = totalSize,
            bytesReceived = existingSize,
            isComplete = existingSize == totalSize
        )
    }

    fun getResumeOffset(transferId: String): Long {
        return activeTransfers[transferId]?.bytesReceived ?: 0L
    }

    fun recordChunkWritten(transferId: String, bytesWritten: Long) {
        val state = activeTransfers[transferId] ?: return
        val newReceived = state.bytesReceived + bytesWritten
        activeTransfers[transferId] = state.copy(
            bytesReceived = newReceived,
            isComplete = newReceived >= state.totalSize
        )
    }
}
