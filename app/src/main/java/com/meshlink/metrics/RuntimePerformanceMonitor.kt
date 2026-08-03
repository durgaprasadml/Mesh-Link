package com.meshlink.metrics

import com.meshlink.BuildConfig
import com.meshlink.common.pool.BufferPool
import com.meshlink.routing.engine.TransportMetrics
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class RuntimeMetricsSnapshot(
    val heapUsedMb: Long,
    val heapMaxMb: Long,
    val heapFreeMb: Long,
    val heapAllocationPct: Float,
    val activeTransfers: Int,
    val activeSessions: Int,
    val queueDepth: Int,
    val activeThreads: Int,
    val activeCoroutines: Int,
    val bufferPoolBorrowedCount: Int,
    val bufferPoolHitCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Lightweight, thread-safe runtime performance monitor.
 * Read-only metrics; debug-only by default to ensure zero production overhead.
 */
@Singleton
class RuntimePerformanceMonitor @Inject constructor(
    private val transportMetrics: TransportMetrics
) {
    private val isEnabled = AtomicBoolean(BuildConfig.DEBUG)

    private val _activeTransfers = AtomicInteger(0)
    private val _activeSessions = AtomicInteger(0)
    private val _activeCoroutines = AtomicInteger(0)

    fun setMonitoringEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
    }

    fun isMonitoringEnabled(): Boolean = isEnabled.get()

    fun recordTransferStart() {
        if (isEnabled.get()) _activeTransfers.incrementAndGet()
    }

    fun recordTransferEnd() {
        if (isEnabled.get()) _activeTransfers.decrementAndGet()
    }

    fun recordSessionStart() {
        if (isEnabled.get()) _activeSessions.incrementAndGet()
    }

    fun recordSessionEnd() {
        if (isEnabled.get()) _activeSessions.decrementAndGet()
    }

    fun recordCoroutineStart() {
        if (isEnabled.get()) _activeCoroutines.incrementAndGet()
    }

    fun recordCoroutineEnd() {
        if (isEnabled.get()) _activeCoroutines.decrementAndGet()
    }

    fun snapshot(): RuntimeMetricsSnapshot {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        val usedBytes = totalMemory - freeMemory
        val usedMb = usedBytes / (1024 * 1024)
        val maxMb = maxMemory / (1024 * 1024)
        val freeMb = maxMb - usedMb

        val allocPct = if (maxMemory > 0) (usedBytes.toFloat() / maxMemory.toFloat() * 100f) else 0f
        val activeThreads = Thread.activeCount()

        return RuntimeMetricsSnapshot(
            heapUsedMb = usedMb,
            heapMaxMb = maxMb,
            heapFreeMb = freeMb,
            heapAllocationPct = allocPct.coerceIn(0f, 100f),
            activeTransfers = _activeTransfers.get().coerceAtLeast(0),
            activeSessions = _activeSessions.get().coerceAtLeast(0),
            queueDepth = transportMetrics.queueDepth,
            activeThreads = activeThreads,
            activeCoroutines = _activeCoroutines.get().coerceAtLeast(0),
            bufferPoolBorrowedCount = BufferPool.getActiveBorrowedCount(),
            bufferPoolHitCount = BufferPool.hitCount.get()
        )
    }
}
