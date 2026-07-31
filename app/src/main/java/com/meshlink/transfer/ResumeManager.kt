package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ResumeState(
    val transferId: String,
    val sha256Checksum: String?,
    val totalChunks: Int,
    val ackedChunkIndices: Set<Int>,
    val lastConfirmedChunkIndex: Int,
    val resumeCount: Int = 0,
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    val remainingChunks: List<Int>
        get() = (0 until totalChunks).filter { !ackedChunkIndices.contains(it) }

    val isComplete: Boolean
        get() = ackedChunkIndices.size >= totalChunks
}

@Singleton
class ResumeManager @Inject constructor() {

    companion object {
        private const val TAG = "ResumeManager"
    }

    private val resumeStates = ConcurrentHashMap<String, ResumeState>()

    /**
     * Initializes or recovers the resume tracking state for a transfer session.
     */
    fun initOrRecoverState(
        transferId: String,
        sha256Checksum: String?,
        totalChunks: Int,
        existingAckedIndices: Set<Int> = emptySet()
    ): ResumeState {
        val lastIdx = if (existingAckedIndices.isNotEmpty()) existingAckedIndices.maxOrNull() ?: 0 else -1
        val state = resumeStates.compute(transferId) { _, existing ->
            if (existing != null) {
                existing.copy(
                    sha256Checksum = sha256Checksum ?: existing.sha256Checksum,
                    totalChunks = totalChunks,
                    ackedChunkIndices = existing.ackedChunkIndices + existingAckedIndices,
                    lastConfirmedChunkIndex = maxOf(existing.lastConfirmedChunkIndex, lastIdx),
                    resumeCount = existing.resumeCount + 1,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            } else {
                ResumeState(
                    transferId = transferId,
                    sha256Checksum = sha256Checksum,
                    totalChunks = totalChunks,
                    ackedChunkIndices = existingAckedIndices,
                    lastConfirmedChunkIndex = lastIdx,
                    resumeCount = 0,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            }
        }!!

        MeshLogger.d(TAG, "Resume state for $transferId: ${state.ackedChunkIndices.size}/${state.totalChunks} chunks confirmed (resumeCount=${state.resumeCount})")
        return state
    }

    /**
     * Records an ACKed chunk index for an active session.
     */
    fun recordChunkAck(transferId: String, chunkIndex: Int) {
        resumeStates.computeIfPresent(transferId) { _, state ->
            val updatedIndices = state.ackedChunkIndices + chunkIndex
            val lastIdx = maxOf(state.lastConfirmedChunkIndex, chunkIndex)
            state.copy(
                ackedChunkIndices = updatedIndices,
                lastConfirmedChunkIndex = lastIdx,
                lastUpdatedMs = System.currentTimeMillis()
            )
        }
    }

    /**
     * Retrieves missing chunk indices for retransmission.
     */
    fun getMissingChunks(transferId: String, totalChunks: Int): List<Int> {
        val state = resumeStates[transferId]
        if (state != null) {
            return state.remainingChunks
        }
        return (0 until totalChunks).toList()
    }

    /**
     * Verifies if incoming metadata matches current resume state.
     * If SHA-256 changes, resume state must be reset to avoid corrupt assembly.
     */
    fun isResumeMetadataValid(transferId: String, newSha256: String?): Boolean {
        val state = resumeStates[transferId] ?: return true
        if (state.sha256Checksum != null && newSha256 != null && state.sha256Checksum != newSha256) {
            MeshLogger.w(TAG, "Transfer $transferId metadata/sha256 changed! Invalidating resume state.")
            clearState(transferId)
            return false
        }
        return true
    }

    fun getResumeState(transferId: String): ResumeState? = resumeStates[transferId]

    fun clearState(transferId: String) {
        resumeStates.remove(transferId)
    }
}
