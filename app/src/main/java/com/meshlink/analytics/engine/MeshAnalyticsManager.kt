package com.meshlink.analytics.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MeshAnalytics(
    val connectedPeers: Int = 0,
    val discoveredPeers: Int = 0,
    val averageRttMs: Long = 0,
    val packetDeliveryRate: Float = 1.0f,
    val packetLossRate: Float = 0.0f,
    val totalHopCount: Int = 0,
    val duplicatePacketRate: Float = 0.0f,
    val averageThroughputKbps: Float = 0.0f,
    val sessionDurationMs: Long = 0
)

@Singleton
class MeshAnalyticsManager @Inject constructor() {

    private val _analytics = MutableStateFlow(MeshAnalytics())
    val analytics: StateFlow<MeshAnalytics> = _analytics

    // In a real implementation, this would aggregate data from RoutingAnalytics, TransportAnalytics, etc.
    fun updateAnalytics(modifier: (MeshAnalytics) -> MeshAnalytics) {
        _analytics.value = modifier(_analytics.value)
    }

    fun recordRtt(rttMs: Long) {
        val current = _analytics.value
        val newAvg = if (current.averageRttMs == 0L) rttMs else (current.averageRttMs + rttMs) / 2
        updateAnalytics { it.copy(averageRttMs = newAvg) }
    }
    
    fun recordPacketLoss(lossDetected: Boolean) {
        // Mock implementation of rolling packet loss average
    }
}
