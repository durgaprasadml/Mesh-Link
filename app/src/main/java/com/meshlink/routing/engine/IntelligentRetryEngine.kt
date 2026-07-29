package com.meshlink.routing.engine


import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.random.Random

@Singleton
class IntelligentRetryEngine @Inject constructor(
    private val congestionMonitor: CongestionMonitor,
    private val batteryAwareNetworking: BatteryAwareNetworking
) {

    /**
     * Calculates the next retry delay dynamically based on network state and retry schedule.
     * @param attempt The current retry attempt (1-indexed or 0-indexed).
     * @return Delay in milliseconds.
     */
    fun calculateRetryDelay(attempt: Int): Long {
        val baseDelay = when {
            attempt <= 1 -> 0L
            attempt == 2 -> 2_000L
            attempt == 3 -> 5_000L
            attempt == 4 -> 10_000L
            attempt == 5 -> 20_000L
            attempt == 6 -> 40_000L
            attempt == 7 -> 60_000L
            else -> 300_000L // 5 minutes
        }

        if (baseDelay == 0L) return 0L

        var scaledDelay = baseDelay
        if (congestionMonitor.isCongested()) {
            scaledDelay *= 2
        }
        if (batteryAwareNetworking.powerState.value == PowerState.CRITICAL) {
            scaledDelay *= 2
        }

        // Add 0-30% randomized jitter
        val jitter = (Random.nextFloat() * 0.3f * scaledDelay).toLong()
        return minOf(300_000L, scaledDelay + jitter)
    }
    
    /**
     * Determines if we should attempt to retry a packet right now.
     */
    fun shouldRetryNow(): Boolean {
        if (batteryAwareNetworking.powerState.value == PowerState.CRITICAL && congestionMonitor.isCongested()) {
            return false
        }
        if (congestionMonitor.congestionLevel.value == CongestionLevel.CRITICAL) {
            return false
        }
        return true
    }
}
