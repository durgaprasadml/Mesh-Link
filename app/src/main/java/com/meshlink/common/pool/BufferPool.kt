package com.meshlink.common.pool

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe memory pool for reusing ByteArrays.
 * Reduces Garbage Collection churn and OutOfMemoryErrors in high-throughput paths.
 */
object BufferPool {
    private val TAG = "BufferPool"

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

    /**
     * Borrow a buffer of exact [size].
     * If one is available in the pool, it is returned (dirty).
     * If not, a new one is allocated.
     */
    fun borrowBuffer(size: Int): ByteArray {
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
     * Validates the buffer size. Rejects if pool is full.
     */
    fun returnBuffer(buffer: ByteArray) {
        val size = buffer.size
        val holder = pools.getOrPut(size) { PoolHolder() }

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
     * Clears all pools and resets metrics. 
     * Useful when the application is moving to background to release memory.
     */
    fun trimMemory() {
        pools.clear()
    }
}
