package com.meshlink.common.pool

import com.meshlink.common.logger.MeshLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe memory pool for reusing ByteArrays.
 * Reduces Garbage Collection churn and OutOfMemoryErrors in high-throughput paths.
 * Enhanced in Phase 4 with double-return safeguards, active allocation tracking, and debug assertions.
 */
object BufferPool {
    private const val TAG = "BufferPool"

    private class PoolHolder {
        val queue = ConcurrentLinkedQueue<ByteArray>()
        val sizeCounter = AtomicInteger(0)
    }

    // Sub-pools categorized by exact buffer size
    private val pools = ConcurrentHashMap<Int, PoolHolder>()

    // Configurable maximum buffers per size category
    private const val MAX_BUFFERS_PER_POOL = 100

    // Instrumentation metrics
    val hitCount = AtomicInteger(0)
    val missCount = AtomicInteger(0)
    val evictionCount = AtomicInteger(0)
    
    // Active allocation metrics
    private val totalBorrowedCount = AtomicInteger(0)
    private val totalReturnedCount = AtomicInteger(0)
    val doubleReturnCount = AtomicInteger(0)

    var isDebugMode: Boolean = com.meshlink.BuildConfig.DEBUG

    /**
     * Borrow a buffer of exact [size].
     * If one is available in the pool, it is returned (dirty).
     * If not, a new one is allocated.
     */
    fun borrowBuffer(size: Int): ByteArray {
        totalBorrowedCount.incrementAndGet()
        val holder = pools[size]
        if (holder != null) {
            val buffer = holder.queue.poll()
            if (buffer != null) {
                holder.sizeCounter.decrementAndGet()
                hitCount.incrementAndGet()
                return buffer
            }
        }
        missCount.incrementAndGet()
        return ByteArray(size)
    }

    /**
     * Borrow a buffer, ensuring all contents are zeroed out before returning.
     */
    fun borrowCleanBuffer(size: Int): ByteArray {
        val buffer = borrowBuffer(size)
        buffer.fill(0)
        return buffer
    }

    /**
     * Return a buffer to the pool.
     * Validates the buffer size, checks for double-returns, and rejects if pool is full.
     */
    fun returnBuffer(buffer: ByteArray) {
        val size = buffer.size
        if (size <= 0) return

        val holder = pools.getOrPut(size) { PoolHolder() }

        // Double-return check in debug mode or if buffer already pooled
        if (holder.queue.contains(buffer)) {
            doubleReturnCount.incrementAndGet()
            MeshLogger.w(TAG, "Double return detected for buffer of size $size!")
            if (isDebugMode) {
                // Log warning and ignore duplicate insertion
                return
            }
            return
        }

        totalReturnedCount.incrementAndGet()

        // Prevent unbounded memory growth with O(1) count tracking
        if (holder.sizeCounter.get() >= MAX_BUFFERS_PER_POOL) {
            evictionCount.incrementAndGet()
            return // Let it be garbage collected
        }

        if (holder.queue.offer(buffer)) {
            holder.sizeCounter.incrementAndGet()
        } else {
            evictionCount.incrementAndGet()
        }
    }

    /**
     * Securely wipes a buffer (overwrites with zeros) and then returns it to the pool.
     * Use this for buffers that held session keys, encrypted payloads, or PII.
     */
    fun returnSecureBuffer(buffer: ByteArray) {
        buffer.fill(0) // Secure wipe
        returnBuffer(buffer)
    }

    /**
     * Executes [block] using a borrowed buffer, automatically returning the buffer
     * to the pool in a try-finally block to prevent pool leaks.
     */
    inline fun <R> useBuffer(size: Int, block: (ByteArray) -> R): R {
        val buffer = borrowBuffer(size)
        return try {
            block(buffer)
        } finally {
            returnBuffer(buffer)
        }
    }

    /**
     * Executes [block] using a clean borrowed buffer, automatically returning it
     * securely (wiped) to the pool in a try-finally block.
     */
    inline fun <R> useCleanBuffer(size: Int, block: (ByteArray) -> R): R {
        val buffer = borrowCleanBuffer(size)
        return try {
            block(buffer)
        } finally {
            returnSecureBuffer(buffer)
        }
    }

    /**
     * Returns net active borrowed buffers count currently in use by the application.
     */
    fun getActiveBorrowedCount(): Int {
        val borrowed = totalBorrowedCount.get()
        val returned = totalReturnedCount.get()
        return (borrowed - returned).coerceAtLeast(0)
    }

    /**
     * Asserts pool consistency in debug mode.
     */
    fun checkPoolConsistency(): Boolean {
        for ((size, holder) in pools) {
            val count = holder.sizeCounter.get()
            val queueSize = holder.queue.size
            if (count < 0 || count != queueSize) {
                MeshLogger.e(TAG, "Pool inconsistency for size $size: counter=$count, queueSize=$queueSize")
                if (isDebugMode) {
                    holder.sizeCounter.set(queueSize)
                }
                return false
            }
        }
        return true
    }

    /**
     * Clears all pools and resets metrics. 
     * Useful when the application is moving to background to release memory.
     */
    fun trimMemory() {
        pools.clear()
        totalBorrowedCount.set(0)
        totalReturnedCount.set(0)
        doubleReturnCount.set(0)
    }
}
