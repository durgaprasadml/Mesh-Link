package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.routing.engine.TransportMetrics
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Selective retransmission scheduler.
 * Periodically audits unACKed chunks in the send window for timeout,
 * and issues targeted retransmission tasks without restarting the transfer.
 */
@Singleton
class ChunkRetransmissionScheduler @Inject constructor(
    private val config: TransferConfiguration,
    private val slidingWindowManager: SlidingWindowManager,
    private val runtimeStateRegistry: TransferRuntimeStateRegistry,
    private val chunkDispatcher: ChunkDispatcher,
    private val metrics: TransportMetrics,
    private val diagnostics: TransportDiagnostics,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "ChunkRetransmissionScheduler"
        private const val MONITOR_CHECK_INTERVAL_MS = 500L
    }

    private val monitorJobs = ConcurrentHashMap<String, Job>()
    private val retransmissionAttempts = ConcurrentHashMap<String, ConcurrentHashMap<Int, Int>>()

    fun startMonitoring(
        session: TransferSession,
        file: File,
        onSendPacket: (suspend (MeshPacket) -> Unit)?,
        onFailure: suspend (reason: String) -> Unit
    ) {
        val transferId = session.transferId
        stopMonitoring(transferId)

        val timeoutMs = config.getAckTimeoutMs(session.transportUsed)

        val job = applicationScope.launch(ioDispatcher + SupervisorJob()) {
            val sessionAttempts = retransmissionAttempts.computeIfAbsent(transferId) { ConcurrentHashMap() }

            while (isActive) {
                delay(MONITOR_CHECK_INTERVAL_MS)

                val runtimeState = runtimeStateRegistry.getState(transferId) ?: continue
                val state = session.state

                if (state != TransferState.STREAMING && state != TransferState.SENDING) {
                    break
                }

                val timedOutChunks = runtimeState.getTimedOutChunks(timeoutMs)
                if (timedOutChunks.isEmpty()) continue

                for (chunkIndex in timedOutChunks) {
                    val currentAttempts = sessionAttempts.getOrDefault(chunkIndex, 0)
                    if (currentAttempts >= config.retryLimit) {
                        MeshLogger.w(TAG, "Chunk $chunkIndex for $transferId exceeded retry limit (${config.retryLimit}). Failing session.")
                        onFailure("Exceeded retry limit on chunk $chunkIndex")
                        return@launch
                    }

                    sessionAttempts[chunkIndex] = currentAttempts + 1
                    runtimeState.retransmissionCount.incrementAndGet()
                    metrics.recordWifiRetry()
                    diagnostics.logTimeout(transferId, chunkIndex)
                    diagnostics.logRetransmission(transferId, chunkIndex, "ACK timeout")

                    // Re-dispatch chunk task
                    chunkDispatcher.dispatchAvailableChunks(session, file, onSendPacket)
                }
            }
        }

        monitorJobs[transferId] = job
    }

    fun stopMonitoring(transferId: String) {
        monitorJobs.remove(transferId)?.cancel()
        retransmissionAttempts.remove(transferId)
    }

    fun clearAll() {
        monitorJobs.values.forEach { it.cancel() }
        monitorJobs.clear()
        retransmissionAttempts.clear()
    }
}
