package com.meshlink.simulator.node

import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.routing.engine.CongestionLevel

/**
 * Pure-JVM stub for IntelligentRetryEngine.
 * Removes the dependency on BatteryAwareNetworking (Android Context)
 * while preserving retry-backoff semantics used by [SimulatedNode].
 */
class IntelligentRetryEngineStub(private val congestionMonitor: CongestionMonitor) {
    fun shouldRetryNow(): Boolean =
        congestionMonitor.congestionLevel.value != CongestionLevel.CRITICAL

    fun calculateRetryDelay(attempt: Int): Long = minOf(120_000L, 2000L * (1 shl attempt))
}
