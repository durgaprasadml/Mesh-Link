package com.meshlink.common.performance

import com.meshlink.common.pool.BufferPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PerformanceDiagnostics(
    val usedMemoryMb: Long = 0L,
    val maxMemoryMb: Long = 0L,
    val freeMemoryMb: Long = 0L,
    val bufferHits: Int = 0,
    val bufferMisses: Int = 0,
    val bufferEvictions: Int = 0
)

@Singleton
class PerformanceMonitor @Inject constructor() {

    private val _diagnostics = MutableStateFlow(PerformanceDiagnostics())
    val diagnostics: StateFlow<PerformanceDiagnostics> = _diagnostics.asStateFlow()

    fun updateMetrics() {
        val runtime = Runtime.getRuntime()
        val totalMem = runtime.totalMemory()
        val freeMem = runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val usedMem = totalMem - freeMem

        val mb = 1024L * 1024L
        _diagnostics.value = PerformanceDiagnostics(
            usedMemoryMb = usedMem / mb,
            maxMemoryMb = maxMem / mb,
            freeMemoryMb = freeMem / mb,
            bufferHits = BufferPool.hitCount.get(),
            bufferMisses = BufferPool.missCount.get(),
            bufferEvictions = BufferPool.evictionCount.get()
        )
    }

    fun trimMemory() {
        BufferPool.trimMemory()
        updateMetrics()
    }
}
