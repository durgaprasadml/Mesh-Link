package com.meshlink.transfer

import com.meshlink.domain.model.MeshPacket
import com.meshlink.transfer.scheduler.SlidingWindowBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Result data holder when a window advances upon ACK processing.
 */
data class WindowAdvanceResult(
    val advancedCount: Int,
    val oldBase: Int,
    val newBase: Int,
    val isComplete: Boolean
)

/**
 * Thread-safe Sliding Window Engine.
 * Manages send and receive window boundaries, tracks in-flight and ACKed chunks,
 * and advances window bounds dynamically upon ACK receipt.
 */
@Singleton
class SlidingWindowManager @Inject constructor(
    private val config: TransferConfiguration,
    private val runtimeStateRegistry: TransferRuntimeStateRegistry
) : SlidingWindowBuffer {

    private class WindowState(
        val transferId: String,
        var windowSize: Int,
        val totalChunks: Int
    ) {
        val lock = ReentrantLock()
        var base: Int = 0
        var nextSeqNum: Int = 0
        val unacknowledgedPackets = ConcurrentHashMap<Int, MeshPacket>()
    }

    private val activeWindows = ConcurrentHashMap<String, WindowState>()

    // Global buffer size default for fallback interface implementation
    private var defaultBufferWindowSize: Int = 16

    // ─────────────────── SlidingWindowBuffer Interface Implementation ───────────────────

    override fun initializeWindow(windowSize: Int) {
        this.defaultBufferWindowSize = windowSize
    }

    override fun addUnacknowledgedChunk(chunkIndex: Int, packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        val state = activeWindows[transferId] ?: return
        state.lock.withLock {
            state.unacknowledgedPackets[chunkIndex] = packet
            runtimeStateRegistry.getState(transferId)?.recordChunkSent(chunkIndex)
        }
    }

    override fun acknowledgeChunk(chunkIndex: Int): Boolean {
        // Fallback interface method across any active session containing chunkIndex
        for (state in activeWindows.values) {
            val result = onAckReceived(state.transferId, chunkIndex)
            if (result.advancedCount > 0 || state.unacknowledgedPackets.containsKey(chunkIndex)) {
                return true
            }
        }
        return false
    }

    override fun getExpiredChunks(timeoutMs: Long): List<MeshPacket> {
        val expired = mutableListOf<MeshPacket>()
        val now = System.currentTimeMillis()

        for (state in activeWindows.values) {
            state.lock.withLock {
                val runtimeState = runtimeStateRegistry.getState(state.transferId) ?: return@withLock
                val timedOutIndices = runtimeState.getTimedOutChunks(timeoutMs)
                for (idx in timedOutIndices) {
                    val pkt = state.unacknowledgedPackets[idx]
                    if (pkt != null) {
                        expired.add(pkt)
                    }
                }
            }
        }
        return expired
    }

    override fun advanceWindow(): Int {
        var totalAdvanced = 0
        for (state in activeWindows.values) {
            state.lock.withLock {
                val runtimeState = runtimeStateRegistry.getState(state.transferId) ?: return@withLock
                val oldBase = state.base
                while (state.base < state.totalChunks && runtimeState.isChunkAcked(state.base)) {
                    state.unacknowledgedPackets.remove(state.base)
                    state.base++
                }
                totalAdvanced += (state.base - oldBase)
            }
        }
        return totalAdvanced
    }

    // ─────────────────── Session-Aware Window Management ───────────────────

    fun initializeSessionWindow(transferId: String, transportType: TransportType, totalChunks: Int): Int {
        val size = config.getWindowSize(transportType)
        val windowState = WindowState(transferId, size, totalChunks)
        activeWindows[transferId] = windowState
        runtimeStateRegistry.getOrCreateState(transferId, size)
        return size
    }

    fun canSend(transferId: String, chunkIndex: Int): Boolean {
        val state = activeWindows[transferId] ?: return false
        state.lock.withLock {
            return chunkIndex >= state.base && chunkIndex < (state.base + state.windowSize) && chunkIndex < state.totalChunks
        }
    }

    fun getNextSendableIndices(transferId: String, maxBatchSize: Int = config.dispatchBatchSize): List<Int> {
        val state = activeWindows[transferId] ?: return emptyList()
        val result = mutableListOf<Int>()
        state.lock.withLock {
            val runtimeState = runtimeStateRegistry.getState(transferId) ?: return emptyList()
            val maxBound = minOf(state.base + state.windowSize, state.totalChunks)
            
            for (idx in state.base until maxBound) {
                if (!runtimeState.isChunkAcked(idx) && !runtimeState.isChunkInFlight(idx)) {
                    result.add(idx)
                    if (result.size >= maxBatchSize) break
                }
            }
        }
        return result
    }

    fun onAckReceived(transferId: String, chunkIndex: Int): WindowAdvanceResult {
        val state = activeWindows[transferId] ?: return WindowAdvanceResult(0, 0, 0, false)
        state.lock.withLock {
            val runtimeState = runtimeStateRegistry.getState(transferId)
            runtimeState?.recordChunkAcked(chunkIndex)
            state.unacknowledgedPackets.remove(chunkIndex)

            val oldBase = state.base
            while (state.base < state.totalChunks && (runtimeState?.isChunkAcked(state.base) == true)) {
                state.unacknowledgedPackets.remove(state.base)
                state.base++
            }

            val advancedCount = state.base - oldBase
            val isComplete = state.base >= state.totalChunks

            return WindowAdvanceResult(
                advancedCount = advancedCount,
                oldBase = oldBase,
                newBase = state.base,
                isComplete = isComplete
            )
        }
    }

    fun onCumulativeAck(transferId: String, highestAckIndex: Int): WindowAdvanceResult {
        val state = activeWindows[transferId] ?: return WindowAdvanceResult(0, 0, 0, false)
        state.lock.withLock {
            val runtimeState = runtimeStateRegistry.getState(transferId)
            val limit = minOf(highestAckIndex, state.totalChunks - 1)
            for (idx in 0..limit) {
                runtimeState?.recordChunkAcked(idx)
                state.unacknowledgedPackets.remove(idx)
            }

            val oldBase = state.base
            if (limit + 1 > state.base) {
                state.base = limit + 1
            }

            while (state.base < state.totalChunks && (runtimeState?.isChunkAcked(state.base) == true)) {
                state.unacknowledgedPackets.remove(state.base)
                state.base++
            }

            val advancedCount = state.base - oldBase
            val isComplete = state.base >= state.totalChunks

            return WindowAdvanceResult(
                advancedCount = advancedCount,
                oldBase = oldBase,
                newBase = state.base,
                isComplete = isComplete
            )
        }
    }

    fun getBase(transferId: String): Int {
        return activeWindows[transferId]?.lock?.withLock { activeWindows[transferId]?.base ?: 0 } ?: 0
    }

    fun getWindowSize(transferId: String): Int {
        return activeWindows[transferId]?.windowSize ?: defaultBufferWindowSize
    }

    fun updateWindowSize(transferId: String, newSize: Int) {
        activeWindows[transferId]?.lock?.withLock {
            activeWindows[transferId]?.windowSize = newSize
        }
    }

    fun closeWindow(transferId: String) {
        activeWindows.remove(transferId)
        runtimeStateRegistry.removeState(transferId)
    }

    fun clearAll() {
        activeWindows.clear()
        runtimeStateRegistry.clear()
    }
}
