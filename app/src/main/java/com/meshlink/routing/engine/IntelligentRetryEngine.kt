package com.meshlink.routing.engine

import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.RouteType
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
     * Calculates retry delay incorporating exponential backoff with randomized jitter,
     * network quality, packet priority, transport type, and congestion level.
     */
    fun calculateAdaptiveRetryDelay(
        attempt: Int,
        priority: PacketPriority = PacketPriority.NORMAL,
        routeType: RouteType = RouteType.BLE
    ): Long {
        if (attempt <= 0) return 0L

        // Exponential backoff base: 1s * 2^(attempt - 1)
        val exponentialFactor = 2.0.pow((attempt - 1).coerceAtMost(6)).toLong()
        var baseDelay = 1000L * exponentialFactor

        // Priority scaling: SOS/CRITICAL gets faster retries
        baseDelay = when (priority) {
            PacketPriority.CRITICAL -> (baseDelay * 0.5f).toLong()
            PacketPriority.HIGH -> (baseDelay * 0.75f).toLong()
            PacketPriority.NORMAL -> baseDelay
            PacketPriority.LOW -> (baseDelay * 1.5f).toLong()
            PacketPriority.BACKGROUND -> (baseDelay * 2.0f).toLong()
        }

        // Transport scaling: Wi-Fi Direct can retry faster than BLE
        if (routeType == RouteType.WIFI_DIRECT) {
            baseDelay = (baseDelay * 0.7f).toLong()
        }

        // Congestion scaling
        if (congestionMonitor.isCongested()) {
            baseDelay = (baseDelay * 2.0f).toLong()
        }

        // Battery scaling
        if (batteryAwareNetworking.powerState.value == PowerState.CRITICAL) {
            baseDelay = (baseDelay * 2.0f).toLong()
        }

        // Randomized Jitter (0-50% full jitter) to break synchronized retry collisions
        val jitter = (Random.nextFloat() * 0.5f * baseDelay).toLong()
        val totalDelay = baseDelay + jitter

        return totalDelay.coerceIn(500L, 300_000L) // Min 500ms, Max 5 mins
    }

    fun calculateRetryDelay(attempt: Int): Long {
        return calculateAdaptiveRetryDelay(attempt, PacketPriority.NORMAL, RouteType.BLE)
    }

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
