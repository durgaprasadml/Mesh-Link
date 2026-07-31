package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class TransferQueueScheduler @Inject constructor() {

    companion object {
        private const val TAG = "TransferQueueScheduler"

        // Concurrency limits per transport to prevent link starvation
        const val MAX_CONCURRENT_BLE_TRANSFERS = 2
        const val MAX_CONCURRENT_WIFI_TRANSFERS = 4

        // Sliding window bounds per transport
        const val MIN_BLE_WINDOW = 2
        const val INITIAL_BLE_WINDOW = 3
        const val MAX_BLE_WINDOW = 5

        const val MIN_WIFI_WINDOW = 8
        const val INITIAL_WIFI_WINDOW = 16
        const val MAX_WIFI_WINDOW = 32
    }

    private val sessions = ConcurrentHashMap<String, TransferSession>()
    private val windowSizes = ConcurrentHashMap<String, Int>()

    private val _activeSessions = MutableStateFlow<List<TransferSession>>(emptyList())
    val activeSessions: StateFlow<List<TransferSession>> = _activeSessions.asStateFlow()

    fun addSession(session: TransferSession) {
        sessions[session.transferId] = session
        val initialWindow = when (session.transportUsed) {
            TransportType.WIFI_DIRECT -> INITIAL_WIFI_WINDOW
            else -> INITIAL_BLE_WINDOW
        }
        windowSizes[session.transferId] = initialWindow
        session.slidingWindowSize = initialWindow
        publishState()
    }

    fun getSession(transferId: String): TransferSession? = sessions[transferId]

    fun updateSessionState(transferId: String, state: TransferState) {
        sessions[transferId]?.let { session ->
            session.state = state
            session.lastUpdatedMs = System.currentTimeMillis()
            if (state == TransferState.COMPLETED || state == TransferState.FAILED || state == TransferState.CANCELLED) {
                session.endTimeMs = System.currentTimeMillis()
            }
            publishState()
        }
    }

    fun updateSessionProgress(transferId: String, chunksTransferred: Int, bytesTransferred: Long) {
        sessions[transferId]?.let { session ->
            session.chunksTransferred = chunksTransferred
            session.bytesTransferred = bytesTransferred
            session.lastUpdatedMs = System.currentTimeMillis()
            publishState()
        }
    }

    fun incrementRetry(transferId: String) {
        sessions[transferId]?.let { session ->
            session.retries++
            // Contract sliding window on retry/loss
            adjustSlidingWindow(transferId, expand = false)
        }
    }

    fun recordAckArrival(transferId: String) {
        // Expand sliding window on successful ACK arrival
        adjustSlidingWindow(transferId, expand = true)
    }

    private fun adjustSlidingWindow(transferId: String, expand: Boolean) {
        val session = sessions[transferId] ?: return
        val minW = if (session.transportUsed == TransportType.WIFI_DIRECT) MIN_WIFI_WINDOW else MIN_BLE_WINDOW
        val maxW = if (session.transportUsed == TransportType.WIFI_DIRECT) MAX_WIFI_WINDOW else MAX_BLE_WINDOW

        windowSizes.compute(transferId) { _, current ->
            val cur = current ?: minW
            val next = if (expand) min(maxW, cur + 1) else max(minW, cur / 2)
            session.slidingWindowSize = next
            next
        }
    }

    fun getSlidingWindowSize(transferId: String): Int {
        val session = sessions[transferId]
        val minW = if (session?.transportUsed == TransportType.WIFI_DIRECT) MIN_WIFI_WINDOW else MIN_BLE_WINDOW
        return windowSizes.getOrDefault(transferId, minW)
    }

    /**
     * Determines whether a specific session is allowed to dispatch its next chunk.
     * Enforces concurrency limits and priority-weighted round robin scheduling.
     */
    fun canSendNextChunk(transferId: String): Boolean {
        val me = sessions[transferId] ?: return false
        if (me.state != TransferState.SENDING) return false

        // 1. Critical priority (SOS) bypasses queue caps
        if (me.priority == TransferPriority.CRITICAL) return true

        // 2. If any SOS critical transfer is active, pause non-critical sends
        val activeOutbound = sessions.values
            .filter { it.direction == TransferDirection.OUTGOING && it.state == TransferState.SENDING }
            .sortedByDescending { it.priority.value }

        if (activeOutbound.any { it.priority == TransferPriority.CRITICAL && it.transferId != transferId }) {
            return false
        }

        // 3. Concurrency limit based on transport
        val sameTransportActive = activeOutbound.filter { it.transportUsed == me.transportUsed }
        val maxAllowed = if (me.transportUsed == TransportType.WIFI_DIRECT) {
            MAX_CONCURRENT_WIFI_TRANSFERS
        } else {
            MAX_CONCURRENT_BLE_TRANSFERS
        }

        val allowedSubset = sameTransportActive.take(maxAllowed)
        return allowedSubset.any { it.transferId == transferId }
    }

    fun removeSession(transferId: String) {
        sessions.remove(transferId)
        windowSizes.remove(transferId)
        publishState()
    }

    private fun publishState() {
        _activeSessions.update { sessions.values.toList() }
    }
}
