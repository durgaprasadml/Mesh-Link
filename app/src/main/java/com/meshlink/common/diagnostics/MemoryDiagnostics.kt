package com.meshlink.common.diagnostics

import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

data class MemoryHeapSnapshot(
    val usedMemoryMb: Long,
    val totalMemoryMb: Long,
    val maxMemoryMb: Long,
    val freeMemoryMb: Long
)

data class BufferPoolSnapshot(
    val hitCount: Int,
    val missCount: Int,
    val evictionCount: Int,
    val activeBorrowedCount: Int
)

/**
 * Debug-only memory diagnostics helper.
 * Provides runtime heap usage snapshots, buffer pool statistics, and active memory allocation monitoring.
 * Guarded to ensure zero production overhead when debug mode is disabled.
 */
@Singleton
class MemoryDiagnostics @Inject constructor() {

    companion object {
        private const val TAG = "MemoryDiagnostics"
        var isDebugMode: Boolean = com.meshlink.BuildConfig.DEBUG
    }

    /**
     * Captures a snapshot of JVM heap memory statistics in megabytes.
     */
    fun getHeapUsageSnapshot(): MemoryHeapSnapshot {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val max = runtime.maxMemory()
        val used = total - free
        val mb = 1024L * 1024L

        return MemoryHeapSnapshot(
            usedMemoryMb = used / mb,
            totalMemoryMb = total / mb,
            maxMemoryMb = max / mb,
            freeMemoryMb = free / mb
        )
    }

    /**
     * Captures a snapshot of BufferPool statistics.
     */
    fun getBufferPoolMetrics(): BufferPoolSnapshot {
        return BufferPoolSnapshot(
            hitCount = BufferPool.hitCount.get(),
            missCount = BufferPool.missCount.get(),
            evictionCount = BufferPool.evictionCount.get(),
            activeBorrowedCount = BufferPool.getActiveBorrowedCount()
        )
    }

    /**
     * Logs structured runtime memory and buffer metrics (DEBUG mode only).
     */
    fun logMemoryDiagnostics(activeSessionsCount: Int = 0, cacheSizeBytes: Long = 0L) {
        if (!isDebugMode) return
        val heap = getHeapUsageSnapshot()
        val pool = getBufferPoolMetrics()
        val cacheMb = cacheSizeBytes / (1024L * 1024L)

        MeshLogger.d(
            TAG,
            "MEMORY_DIAGNOSTICS | Heap: Used=${heap.usedMemoryMb}MB / Max=${heap.maxMemoryMb}MB | " +
            "Pool: Hits=${pool.hitCount}, Misses=${pool.missCount}, Evictions=${pool.evictionCount}, ActiveBorrowed=${pool.activeBorrowedCount} | " +
            "ActiveSessions=$activeSessionsCount | Cache=${cacheMb}MB"
        )
    }
}
