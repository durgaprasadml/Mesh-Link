package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class TransferScheduler @Inject constructor() {
    companion object {
        private const val TAG = "TransferScheduler"
        // Maximum concurrent active sending streams to prevent BLE buffer starvation
        private const val MAX_CONCURRENT_SENDS = 2
        // Minimum interval between progress StateFlow emissions to prevent excessive Compose recompositions.
        // State changes (COMPLETED, FAILED) always emit immediately regardless of this throttle.
        private const val PROGRESS_EMIT_INTERVAL_MS = 150L
    }

    // Per-transfer last-emit timestamp for throttling progress updates
    private val lastProgressEmitMs = ConcurrentHashMap<String, Long>()

    private val sessions = ConcurrentHashMap<String, TransferSession>()
    
    private val _activeSessions = MutableStateFlow<List<TransferSession>>(emptyList())
    val activeSessions: StateFlow<List<TransferSession>> = _activeSessions.asStateFlow()

    fun addSession(session: TransferSession) {
        sessions[session.transferId] = session
        publishState()
    }

    fun getSession(transferId: String): TransferSession? {
        return sessions[transferId]
    }

    fun updateSessionState(transferId: String, state: TransferState) {
        sessions[transferId]?.let {
            it.state = state
            if (state == TransferState.COMPLETED || state == TransferState.FAILED || state == TransferState.CANCELLED) {
                it.endTimeMs = System.currentTimeMillis()
            }
            publishState()
        }
    }

    fun updateSessionProgress(transferId: String, chunksTransferred: Int, bytesTransferred: Long) {
        sessions[transferId]?.let { session ->
            session.chunksTransferred = chunksTransferred
            session.bytesTransferred = bytesTransferred

            // Throttle StateFlow emissions to prevent Compose recomposition on every 180-byte BLE chunk.
            // Always emit on completion (all chunks received) to ensure UI reflects final state promptly.
            val isComplete = session.totalChunks > 0 && chunksTransferred >= session.totalChunks
            val now = System.currentTimeMillis()
            val last = lastProgressEmitMs[transferId] ?: 0L
            if (isComplete || (now - last) >= PROGRESS_EMIT_INTERVAL_MS) {
                lastProgressEmitMs[transferId] = now
                publishState()
            }
        }
    }

    fun incrementRetry(transferId: String) {
        sessions[transferId]?.let {
            it.retries++
        }
    }

    /**
     * Examines the queue and determines if a session is allowed to send its next chunk.
     * Enforces Priority Queue ordering (Critical > High > Medium > Low).
     *
     * Note: TransferManager uses [TransferState.STREAMING] for active BLE chunk transfers,
     * not [TransferState.SENDING]. Both states are accepted here so voice (HIGH priority) correctly
     * preempts concurrent image transfers (MEDIUM priority).
     */
    fun canSendNextChunk(transferId: String): Boolean {
        val me = sessions[transferId] ?: return false
        // Accept both SENDING and STREAMING — TransferManager uses STREAMING for BLE chunked transfers
        val isActiveOutbound = me.state == TransferState.SENDING || me.state == TransferState.STREAMING
        if (!isActiveOutbound) return false

        val activeOutbound = sessions.values
            .filter {
                it.direction == TransferDirection.OUTGOING &&
                    (it.state == TransferState.SENDING || it.state == TransferState.STREAMING)
            }
            .sortedByDescending { it.priority.value }

        // If I am Critical (SOS), I can always send
        if (me.priority == TransferPriority.CRITICAL) return true

        // If there are Critical transfers running, pause all non-critical
        if (activeOutbound.any { it.priority == TransferPriority.CRITICAL && it.transferId != transferId }) {
            return false
        }

        // Take top N by priority
        val allowed = activeOutbound.take(MAX_CONCURRENT_SENDS)
        return allowed.any { it.transferId == transferId }
    }

    /** Clean up throttle tracking when a session ends. */
    fun cleanupProgressTracking(transferId: String) {
        lastProgressEmitMs.remove(transferId)
    }

    private fun publishState() {
        _activeSessions.update { sessions.values.toList() }
    }
}
