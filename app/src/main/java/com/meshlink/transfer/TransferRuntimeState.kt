package com.meshlink.transfer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe container for transient runtime transfer statistics and window state.
 * Decouples runtime metrics from the persistent TransferSession domain model.
 */
class TransferRuntimeState(
    val transferId: String,
    val windowSize: Int
) {
    val activeWorkersCount = AtomicInteger(0)
    val retransmissionCount = AtomicInteger(0)
    val highestAckIndex = AtomicInteger(-1)
    val lastActivityTimestampMs = AtomicLong(System.currentTimeMillis())

    // Maps chunkIndex -> sendTimestampMs
    val inFlightChunks = ConcurrentHashMap<Int, Long>()

    // Maps chunkIndex -> ACKed status
    val ackedChunks = ConcurrentHashMap<Int, Boolean>()

    fun recordChunkSent(chunkIndex: Int) {
        val now = System.currentTimeMillis()
        inFlightChunks[chunkIndex] = now
        lastActivityTimestampMs.set(now)
    }

    fun recordChunkAcked(chunkIndex: Int): Boolean {
        inFlightChunks.remove(chunkIndex)
        val wasAlreadyAcked = ackedChunks.put(chunkIndex, true) == true
        lastActivityTimestampMs.set(System.currentTimeMillis())

        var currentHighest = highestAckIndex.get()
        while (chunkIndex > currentHighest) {
            if (highestAckIndex.compareAndSet(currentHighest, chunkIndex)) {
                break
            }
            currentHighest = highestAckIndex.get()
        }

        return !wasAlreadyAcked
    }

    fun isChunkAcked(chunkIndex: Int): Boolean {
        return ackedChunks[chunkIndex] == true
    }

    fun isChunkInFlight(chunkIndex: Int): Boolean {
        return inFlightChunks.containsKey(chunkIndex)
    }

    fun getTimedOutChunks(timeoutMs: Long): List<Int> {
        val now = System.currentTimeMillis()
        val timedOut = mutableListOf<Int>()
        inFlightChunks.forEach { (index, timestamp) ->
            if (now - timestamp >= timeoutMs && ackedChunks[index] != true) {
                timedOut.add(index)
            }
        }
        return timedOut
    }

    fun getAckedCount(): Int = ackedChunks.size
    fun getInFlightCount(): Int = inFlightChunks.size

    fun reset() {
        activeWorkersCount.set(0)
        retransmissionCount.set(0)
        highestAckIndex.set(-1)
        inFlightChunks.clear()
        ackedChunks.clear()
        lastActivityTimestampMs.set(System.currentTimeMillis())
    }
}

/**
 * Registry/Factory for managing active TransferRuntimeState instances across sessions.
 */
@Singleton
class TransferRuntimeStateRegistry @Inject constructor() {
    private val activeStates = ConcurrentHashMap<String, TransferRuntimeState>()

    fun getOrCreateState(transferId: String, windowSize: Int): TransferRuntimeState {
        return activeStates.computeIfAbsent(transferId) {
            TransferRuntimeState(transferId, windowSize)
        }
    }

    fun getState(transferId: String): TransferRuntimeState? {
        return activeStates[transferId]
    }

    fun removeState(transferId: String): TransferRuntimeState? {
        return activeStates.remove(transferId)
    }

    fun clear() {
        activeStates.clear()
    }
}
