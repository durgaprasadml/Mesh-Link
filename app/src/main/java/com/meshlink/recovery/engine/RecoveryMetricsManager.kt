package com.meshlink.recovery.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class RecoveryMetrics(
    val recoveryAttempts: Int = 0,
    val successfulRecoveries: Int = 0,
    val failedRecoveries: Int = 0,
    val lastRecoveryDurationMs: Long = 0,
    val totalIntegrityFailures: Int = 0,
    val continuityScore: Float = 1.0f
)

@Singleton
class RecoveryMetricsManager @Inject constructor() {

    private val _metrics = MutableStateFlow(RecoveryMetrics())
    val metrics: StateFlow<RecoveryMetrics> = _metrics

    fun recordIntegrityFailure() {
        val current = _metrics.value
        _metrics.value = current.copy(
            totalIntegrityFailures = current.totalIntegrityFailures + 1,
            continuityScore = calculateScore(current.successfulRecoveries, current.failedRecoveries, current.totalIntegrityFailures + 1)
        )
    }

    fun recordRecovery(durationMs: Long, success: Boolean) {
        val current = _metrics.value
        val successCount = if (success) current.successfulRecoveries + 1 else current.successfulRecoveries
        val failCount = if (!success) current.failedRecoveries + 1 else current.failedRecoveries
        
        _metrics.value = current.copy(
            recoveryAttempts = current.recoveryAttempts + 1,
            successfulRecoveries = successCount,
            failedRecoveries = failCount,
            lastRecoveryDurationMs = durationMs,
            continuityScore = calculateScore(successCount, failCount, current.totalIntegrityFailures)
        )
    }

    private fun calculateScore(successes: Int, failures: Int, integrityFails: Int): Float {
        val baseScore = 1.0f
        val penalty = (failures * 0.1f) + (integrityFails * 0.05f)
        return (baseScore - penalty).coerceAtLeast(0f)
    }
}
