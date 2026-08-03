package com.meshlink.metrics

import com.meshlink.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class CoroutineHealthSnapshot(
    val activeJobsCount: Long,
    val completedJobsCount: Long,
    val cancelledJobsCount: Long,
    val failedJobsCount: Long,
    val longRunningJobsCount: Long,
    val suspectedLeaksCount: Long,
    val isStructuredConcurrencyCompliant: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Debug-only coroutine health monitor for detecting leaks, orphan jobs, and scope violations.
 */
@Singleton
class CoroutineDiagnostics @Inject constructor() {

    private val isEnabled = AtomicBoolean(BuildConfig.DEBUG)

    private val activeJobs = AtomicLong(0)
    private val completedJobs = AtomicLong(0)
    private val cancelledJobs = AtomicLong(0)
    private val failedJobs = AtomicLong(0)
    private val longRunningJobs = AtomicLong(0)

    private val jobStartTimes = ConcurrentHashMap<String, Long>()

    fun setDiagnosticsEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
    }

    fun recordJobStart(jobId: String) {
        if (!isEnabled.get()) return
        activeJobs.incrementAndGet()
        jobStartTimes[jobId] = System.currentTimeMillis()
    }

    fun recordJobCompleted(jobId: String, durationMs: Long = 0L) {
        if (!isEnabled.get()) return
        activeJobs.decrementAndGet()
        completedJobs.incrementAndGet()

        val startTime = jobStartTimes.remove(jobId)
        val elapsed = if (durationMs > 0) durationMs else if (startTime != null) System.currentTimeMillis() - startTime else 0L

        if (elapsed > 30_000) { // >30s considered long running
            longRunningJobs.incrementAndGet()
        }
    }

    fun recordJobCancelled(jobId: String) {
        if (!isEnabled.get()) return
        activeJobs.decrementAndGet()
        cancelledJobs.incrementAndGet()
        jobStartTimes.remove(jobId)
    }

    fun recordJobFailed(jobId: String, throwable: Throwable? = null) {
        if (!isEnabled.get()) return
        activeJobs.decrementAndGet()
        failedJobs.incrementAndGet()
        jobStartTimes.remove(jobId)
    }

    fun snapshot(): CoroutineHealthSnapshot {
        val now = System.currentTimeMillis()
        var suspectedLeaks = 0L

        jobStartTimes.forEach { (_, startTime) ->
            if (now - startTime > 120_000) { // >2 minutes active in scope
                suspectedLeaks++
            }
        }

        val active = activeJobs.get().coerceAtLeast(0)
        val isCompliant = suspectedLeaks == 0L

        return CoroutineHealthSnapshot(
            activeJobsCount = active,
            completedJobsCount = completedJobs.get(),
            cancelledJobsCount = cancelledJobs.get(),
            failedJobsCount = failedJobs.get(),
            longRunningJobsCount = longRunningJobs.get(),
            suspectedLeaksCount = suspectedLeaks,
            isStructuredConcurrencyCompliant = isCompliant
        )
    }
}
