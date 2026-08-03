package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.routing.engine.TransportMetrics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result returned upon processing an incoming ACK packet.
 */
data class AckProcessResult(
    val transferId: String,
    val chunkIndex: Int,
    val isDuplicateOrStale: Boolean,
    val windowAdvancedCount: Int,
    val newWindowBase: Int,
    val isTransferComplete: Boolean
)

/**
 * Centralized manager for handling ACK packets.
 * Preserves 100% backward compatibility with existing MEDIA_ACK packet formats and semantics.
 * Prepared internally for future protocol cumulative ACK expansions.
 */
@Singleton
class TransferAckManager @Inject constructor(
    private val slidingWindowManager: SlidingWindowManager,
    private val runtimeStateRegistry: TransferRuntimeStateRegistry,
    private val metrics: TransportMetrics,
    private val diagnostics: TransportDiagnostics
) {
    companion object {
        private const val TAG = "TransferAckManager"
    }

    /**
     * Handles incoming MEDIA_ACK packet for a given transfer session.
     */
    fun processAck(packet: MeshPacket, totalChunks: Int): AckProcessResult? {
        val transferId = packet.transferId ?: return null
        val chunkIndex = packet.chunkIndex

        val runtimeState = runtimeStateRegistry.getState(transferId)
        val isAlreadyAcked = runtimeState?.isChunkAcked(chunkIndex) == true

        if (isAlreadyAcked) {
            diagnostics.logAckReceived(transferId, chunkIndex)
            return AckProcessResult(
                transferId = transferId,
                chunkIndex = chunkIndex,
                isDuplicateOrStale = true,
                windowAdvancedCount = 0,
                newWindowBase = slidingWindowManager.getBase(transferId),
                isTransferComplete = false
            )
        }

        // Process ACK in sliding window
        val windowResult = slidingWindowManager.onAckReceived(transferId, chunkIndex)

        // Metrics and Diagnostics
        metrics.recordWifiRx(0) // Record successful ACK reception activity
        diagnostics.logAckReceived(transferId, chunkIndex)

        if (windowResult.advancedCount > 0) {
            diagnostics.logWindowAdvanced(
                transferId = transferId,
                oldBase = windowResult.oldBase,
                newBase = windowResult.newBase
            )
        }

        // Check completion condition
        val isComplete = windowResult.isComplete || (chunkIndex == totalChunks - 1 && slidingWindowManager.getBase(transferId) >= totalChunks)

        return AckProcessResult(
            transferId = transferId,
            chunkIndex = chunkIndex,
            isDuplicateOrStale = false,
            windowAdvancedCount = windowResult.advancedCount,
            newWindowBase = windowResult.newBase,
            isTransferComplete = isComplete
        )
    }

    /**
     * Process range or cumulative ACKs if present in payload (future compatibility preparation).
     */
    fun processCumulativeAck(transferId: String, highestAckIndex: Int): WindowAdvanceResult {
        val result = slidingWindowManager.onCumulativeAck(transferId, highestAckIndex)
        if (result.advancedCount > 0) {
            diagnostics.logWindowAdvanced(transferId, result.oldBase, result.newBase)
        }
        return result
    }
}
