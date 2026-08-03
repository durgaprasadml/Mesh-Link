package com.meshlink.metrics

import com.meshlink.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryDiagnosticsReport(
    val initialBatteryLevelPct: Int,
    val currentBatteryLevelPct: Int,
    val totalBatteryDrainPct: Int,
    val wakeLockDurationMs: Long,
    val bleRadioActiveMs: Long,
    val wifiRadioActiveMs: Long,
    val foregroundRuntimeMs: Long,
    val backgroundRuntimeMs: Long,
    val bytesTransferredPerBatteryPct: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Debug-only battery diagnostics for tracking energy consumption and radio activity.
 * Guaranteed zero overhead in release builds unless explicitly enabled.
 */
@Singleton
class BatteryDiagnostics @Inject constructor() {

    private val isEnabled = AtomicBoolean(BuildConfig.DEBUG)

    private val initialLevel = AtomicLong(100)
    private val currentLevel = AtomicLong(100)
    private val wakeLockMs = AtomicLong(0)
    private val bleRadioMs = AtomicLong(0)
    private val wifiRadioMs = AtomicLong(0)
    private val foregroundMs = AtomicLong(0)
    private val backgroundMs = AtomicLong(0)
    private val totalBytes = AtomicLong(0)

    fun setDiagnosticsEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
    }

    fun recordBatteryLevel(levelPct: Int) {
        if (!isEnabled.get()) return
        val pct = levelPct.coerceIn(0, 100).toLong()
        currentLevel.set(pct)
    }

    fun recordWakeLock(durationMs: Long) {
        if (!isEnabled.get() || durationMs <= 0) return
        wakeLockMs.addAndGet(durationMs)
    }

    fun recordBleActivity(durationMs: Long) {
        if (!isEnabled.get() || durationMs <= 0) return
        bleRadioMs.addAndGet(durationMs)
    }

    fun recordWifiActivity(durationMs: Long) {
        if (!isEnabled.get() || durationMs <= 0) return
        wifiRadioMs.addAndGet(durationMs)
    }

    fun recordRuntime(foregroundDurationMs: Long, backgroundDurationMs: Long) {
        if (!isEnabled.get()) return
        if (foregroundDurationMs > 0) foregroundMs.addAndGet(foregroundDurationMs)
        if (backgroundDurationMs > 0) backgroundMs.addAndGet(backgroundDurationMs)
    }

    fun recordBytesTransferred(bytes: Long) {
        if (!isEnabled.get() || bytes <= 0) return
        totalBytes.addAndGet(bytes)
    }

    fun generateReport(): BatteryDiagnosticsReport {
        val initial = initialLevel.get().toInt()
        val current = currentLevel.get().toInt()
        val drain = (initial - current).coerceAtLeast(0)

        val bytesPerPct = if (drain > 0) totalBytes.get() / drain else totalBytes.get()

        return BatteryDiagnosticsReport(
            initialBatteryLevelPct = initial,
            currentBatteryLevelPct = current,
            totalBatteryDrainPct = drain,
            wakeLockDurationMs = wakeLockMs.get(),
            bleRadioActiveMs = bleRadioMs.get(),
            wifiRadioActiveMs = wifiRadioMs.get(),
            foregroundRuntimeMs = foregroundMs.get(),
            backgroundRuntimeMs = backgroundMs.get(),
            bytesTransferredPerBatteryPct = bytesPerPct
        )
    }
}
