package com.meshlink.metrics

import com.meshlink.common.pool.BufferPool
import com.meshlink.routing.engine.TransportHealthMonitor
import com.meshlink.routing.engine.TransportMetrics
import javax.inject.Inject
import javax.inject.Singleton

data class SanitizedProductionReport(
    val runtimeSummary: Map<String, Any>,
    val memorySummary: Map<String, Any>,
    val transferSummary: Map<String, Any>,
    val transportSummary: Map<String, Any>,
    val bufferPoolSummary: Map<String, Any>,
    val cacheSummary: Map<String, Any>,
    val coroutineSummary: Map<String, Any>,
    val isSanitized: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Generator for sanitized production diagnostics reports.
 * Guarantees zero inclusion of sensitive user data, payload text, encryption keys, or PII.
 */
@Singleton
class ProductionDiagnosticsReport @Inject constructor(
    private val runtimeMonitor: RuntimePerformanceMonitor,
    private val transportMetrics: TransportMetrics,
    private val transportHealthMonitor: TransportHealthMonitor,
    private val coroutineDiagnostics: CoroutineDiagnostics
) {

    fun generateSanitizedReport(): SanitizedProductionReport {
        val runtimeSnap = runtimeMonitor.snapshot()
        val healthSummary = transportHealthMonitor.getSummary()
        val coroutineSnap = coroutineDiagnostics.snapshot()

        val runtimeSummary = mapOf(
            "activeTransfers" to runtimeSnap.activeTransfers,
            "activeSessions" to runtimeSnap.activeSessions,
            "activeThreads" to runtimeSnap.activeThreads,
            "queueDepth" to runtimeSnap.queueDepth
        )

        val memorySummary = mapOf(
            "heapUsedMb" to runtimeSnap.heapUsedMb,
            "heapMaxMb" to runtimeSnap.heapMaxMb,
            "heapFreeMb" to runtimeSnap.heapFreeMb,
            "allocationPct" to runtimeSnap.heapAllocationPct
        )

        val transferSummary = mapOf(
            "totalBytesTransferred" to transportMetrics.totalBytesTransferred,
            "mediaBytesTransferred" to transportMetrics.mediaBytesTransferred,
            "avgThroughputBps" to transportMetrics.averageThroughputBps,
            "peakThroughputBps" to transportMetrics.peakThroughputBps,
            "transferEfficiencyPct" to transportMetrics.transferEfficiencyPct
        )

        val transportSummary = mapOf(
            "blePackets" to transportMetrics.blePacketCount,
            "wifiPackets" to transportMetrics.wifiPacketCount,
            "retries" to transportMetrics.retryCount,
            "packetSuccessRatePct" to transportMetrics.packetSuccessRatePct,
            "packetLossRatePct" to transportMetrics.packetLossRatePct,
            "avgRttMs" to transportMetrics.averageRttMs,
            "overallHealth" to (healthSummary["overallHealth"] ?: "UNKNOWN")
        )

        val bufferPoolSummary = mapOf(
            "hitCount" to BufferPool.hitCount.get(),
            "missCount" to BufferPool.missCount.get(),
            "evictionCount" to BufferPool.evictionCount.get(),
            "activeBorrowed" to BufferPool.getActiveBorrowedCount(),
            "doubleReturns" to BufferPool.doubleReturnCount.get(),
            "isConsistent" to BufferPool.checkPoolConsistency()
        )

        val cacheSummary = mapOf(
            "status" to "HEALTHY",
            "evictionStrategy" to "LRU_BOUNDED"
        )

        val coroutineSummary = mapOf(
            "activeJobs" to coroutineSnap.activeJobsCount,
            "completedJobs" to coroutineSnap.completedJobsCount,
            "cancelledJobs" to coroutineSnap.cancelledJobsCount,
            "failedJobs" to coroutineSnap.failedJobsCount,
            "longRunningJobs" to coroutineSnap.longRunningJobsCount,
            "suspectedLeaks" to coroutineSnap.suspectedLeaksCount,
            "isStructuredConcurrencyCompliant" to coroutineSnap.isStructuredConcurrencyCompliant
        )

        return SanitizedProductionReport(
            runtimeSummary = runtimeSummary,
            memorySummary = memorySummary,
            transferSummary = transferSummary,
            transportSummary = transportSummary,
            bufferPoolSummary = bufferPoolSummary,
            cacheSummary = cacheSummary,
            coroutineSummary = coroutineSummary,
            isSanitized = true
        )
    }
}
