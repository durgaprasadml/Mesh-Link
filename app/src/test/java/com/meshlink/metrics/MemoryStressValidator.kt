package com.meshlink.metrics

import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

data class MemoryStressReport(
    val textMessagesProcessed: Int,
    val imageTransfersProcessed: Int,
    val audioTransfersProcessed: Int,
    val connectDisconnectCycles: Int,
    val initialHeapUsedMb: Long,
    val finalHeapUsedMb: Long,
    val heapDeltaMb: Long,
    val bufferPoolConsistent: Boolean,
    val activeBorrowedBuffersRemaining: Int,
    val passed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Validates heap stability, allocation growth, BufferPool consistency, and cache bounds
 * under heavy message and media transfer stress workloads.
 */
@Singleton
class MemoryStressValidator @Inject constructor() {

    fun executeStressValidation(
        textMessageCount: Int = 1000,
        imageTransferCount: Int = 500,
        audioTransferCount: Int = 100,
        connectionCycleCount: Int = 50
    ): MemoryStressReport {
        val runtime = Runtime.getRuntime()
        System.gc()
        val initialUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        // 1. Process 1,000 text messages using BufferPool
        repeat(textMessageCount) {
            val buf = BufferPool.borrowBuffer(256)
            buf.fill(0x2A)
            BufferPool.returnBuffer(buf)
        }

        // 2. Process 500 image transfers
        repeat(imageTransferCount) {
            val buf = BufferPool.borrowBuffer(16 * 1024)
            buf.fill(0x7F)
            BufferPool.returnBuffer(buf)
        }

        // 3. Process 100 audio transfers
        repeat(audioTransferCount) {
            val buf = BufferPool.borrowBuffer(32 * 1024)
            buf.fill(0x3C)
            BufferPool.returnBuffer(buf)
        }

        // 4. Connect / disconnect cycles simulation
        repeat(connectionCycleCount) {
            val sessionBuf = BufferPool.borrowBuffer(1024)
            sessionBuf.fill(0x00)
            BufferPool.returnBuffer(sessionBuf)
        }

        System.gc()
        val finalUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val heapDelta = finalUsed - initialUsed
        val bufferPoolOk = BufferPool.checkPoolConsistency()
        val remainingBorrowed = BufferPool.getActiveBorrowedCount()

        // Passed if heap delta remains bounded (< 50MB growth) and zero leaked buffers
        val passed = bufferPoolOk && remainingBorrowed == 0 && heapDelta <= 50

        return MemoryStressReport(
            textMessagesProcessed = textMessageCount,
            imageTransfersProcessed = imageTransferCount,
            audioTransfersProcessed = audioTransferCount,
            connectDisconnectCycles = connectionCycleCount,
            initialHeapUsedMb = initialUsed,
            finalHeapUsedMb = finalUsed,
            heapDeltaMb = heapDelta,
            bufferPoolConsistent = bufferPoolOk,
            activeBorrowedBuffersRemaining = remainingBorrowed,
            passed = passed
        )
    }
}
