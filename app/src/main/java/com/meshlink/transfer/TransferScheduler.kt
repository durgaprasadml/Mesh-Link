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
    }

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
        sessions[transferId]?.let {
            it.chunksTransferred = chunksTransferred
            it.bytesTransferred = bytesTransferred
            publishState()
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
     */
    fun canSendNextChunk(transferId: String): Boolean {
        val me = sessions[transferId] ?: return false
        if (me.state != TransferState.SENDING) return false

        val activeOutbound = sessions.values
            .filter { it.direction == TransferDirection.OUTGOING && it.state == TransferState.SENDING }
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

    private fun publishState() {
        _activeSessions.update { sessions.values.toList() }
    }
}
