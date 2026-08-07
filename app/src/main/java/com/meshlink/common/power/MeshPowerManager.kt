package com.meshlink.common.power

import android.content.Context
import android.os.PowerManager
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshPowerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MeshPowerManager"
        private const val WAKE_LOCK_TAG = "MeshLink::ScopedWakeLock"
        private const val DEFAULT_TIMEOUT_MS = 15_000L
    }

    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    private var totalWakeLockDurationMs: Long = 0L

    fun getTotalWakeLockDurationMs(): Long = totalWakeLockDurationMs

    /**
     * Executes the given block under a temporary PARTIAL_WAKE_LOCK with auto-release.
     */
    suspend fun <T> runWithWakeLock(
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        block: suspend () -> T
    ): T {
        val lock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
            setReferenceCounted(false)
        }
        val startTime = System.currentTimeMillis()
        try {
            lock?.acquire(timeoutMs)
            MeshLogger.d(TAG, "Acquired WakeLock with timeout ${timeoutMs}ms")
            return block()
        } finally {
            if (lock?.isHeld == true) {
                try {
                    lock.release()
                    val duration = System.currentTimeMillis() - startTime
                    totalWakeLockDurationMs += duration
                    MeshLogger.d(TAG, "Released WakeLock after ${duration}ms")
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Failed to release WakeLock: ${e.message}")
                }
            }
        }
    }
}
