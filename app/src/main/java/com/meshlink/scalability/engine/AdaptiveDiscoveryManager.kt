package com.meshlink.scalability.engine

import javax.inject.Inject
import javax.inject.Singleton

enum class DiscoveryState {
    IDLE, SPARSE, DENSE, EMERGENCY
}

@Singleton
class AdaptiveDiscoveryManager @Inject constructor() {

    private var currentState = DiscoveryState.SPARSE

    fun evaluateDiscoveryDutyCycle(activePeerCount: Int, isEmergency: Boolean): DiscoveryState {
        if (isEmergency) {
            currentState = DiscoveryState.EMERGENCY
            return currentState
        }

        currentState = when {
            activePeerCount == 0 -> DiscoveryState.SPARSE
            activePeerCount > 50 -> DiscoveryState.DENSE
            activePeerCount > 10 -> DiscoveryState.IDLE
            else -> DiscoveryState.SPARSE
        }
        
        return currentState
    }

    fun getRecommendedScanIntervalMs(): Long {
        return when (currentState) {
            DiscoveryState.EMERGENCY -> 1000L // Highly aggressive, battery heavy
            DiscoveryState.SPARSE -> 5000L    // Looking for a cluster to join
            DiscoveryState.IDLE -> 15000L     // Relaxed scanning, preserving battery
            DiscoveryState.DENSE -> 30000L    // Heavily throttled to avoid discovery storms
        }
    }
}
