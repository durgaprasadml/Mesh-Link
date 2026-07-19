package com.meshlink.common.power

import android.os.SystemClock
import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PowerMetrics(
    val activeCpuTimeMs: Long,
    val bleScanDurationMs: Long,
    val wakeLockDurationMs: Long,
    val powerScore: Int
)

@Singleton
class PowerMetricsManager @Inject constructor() {
    companion object {
        private const val TAG = "PowerMetrics"
    }

    private val _metrics = MutableStateFlow(PowerMetrics(0, 0, 0, 100))
    val metrics: StateFlow<PowerMetrics> = _metrics.asStateFlow()

    private var sessionStartTime = SystemClock.elapsedRealtime()
    private var totalBleScanTime = 0L
    private var totalWakeLockTime = 0L

    fun recordBleScan(durationMs: Long) {
        totalBleScanTime += durationMs
        updateMetrics()
    }

    fun recordWakeLock(durationMs: Long) {
        totalWakeLockTime += durationMs
        updateMetrics()
    }

    private fun updateMetrics() {
        val cpuActive = SystemClock.elapsedRealtime() - sessionStartTime
        
        // Calculate a crude power score based on radio and cpu uptime
        var score = 100
        val radioRatio = if (cpuActive > 0) totalBleScanTime.toFloat() / cpuActive else 0f
        if (radioRatio > 0.8f) score -= 40
        else if (radioRatio > 0.5f) score -= 20
        
        if (totalWakeLockTime > 300_000L) score -= 20 // Heavy wake lock penalty

        _metrics.value = PowerMetrics(
            activeCpuTimeMs = cpuActive,
            bleScanDurationMs = totalBleScanTime,
            wakeLockDurationMs = totalWakeLockTime,
            powerScore = score.coerceIn(0, 100)
        )
        
        if (score < 50) {
            MeshLogger.w(TAG, "Low Power Score detected: $score. High background usage.")
        }
    }
}
