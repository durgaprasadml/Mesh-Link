package com.meshlink.routing.engine

import com.meshlink.ai.data.LearningRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.random.Random

@Singleton
class IntelligentRetryEngine @Inject constructor(
    private val congestionMonitor: CongestionMonitor,
    private val batteryAwareNetworking: BatteryAwareNetworking,
    private val learningRepository: LearningRepository
) {

    /**
     * Calculates the next retry delay dynamically based on network state and AI learning.
     * @param attempt The current retry attempt (0-indexed).
     * @return Delay in milliseconds.
     */
    fun calculateRetryDelay(attempt: Int): Long {
        // AI component: If retries generally fail a lot globally, increase the base penalty to save battery
        val globalRetrySuccess = learningRepository.getGlobalMetric("avg_retry_success", 0.5f)
        
        // Base delay is 2 seconds, but scaled inverse to historical success
        var baseDelay = (2000L * (1.0f + (1.0f - globalRetrySuccess)) * (2.0.pow(attempt.toDouble()))).toLong()
        
        // If congested, dramatically increase the backoff
        if (congestionMonitor.isCongested()) {
            baseDelay *= 3 
        }

        // If battery is critical, stretch retries to save wakeups
        if (batteryAwareNetworking.powerState.value == PowerState.CRITICAL) {
            baseDelay *= 2
        }

        // Add 0-30% jitter to prevent thundering herd
        val jitter = (Random.nextFloat() * 0.3 * baseDelay).toLong()
        
        // Max delay 2 minutes
        return Math.min(120_000L, baseDelay + jitter)
    }
    
    /**
     * Determines if we should even attempt to retry a packet right now.
     */
    fun shouldRetryNow(): Boolean {
        if (congestionMonitor.congestionLevel.value == CongestionLevel.CRITICAL) {
            // Drop retries completely if the network is critically overloaded
            return false
        }
        return true
    }
}
