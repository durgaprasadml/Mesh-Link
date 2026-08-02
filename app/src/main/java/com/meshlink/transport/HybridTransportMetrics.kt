package com.meshlink.transport

/**
 * Diagnostic metrics exposed via StateFlow for monitoring performance, fallbacks, and utilization.
 */
data class HybridTransportMetrics(
    val activeMode: HybridMode = HybridMode.BLE_ONLY,
    val throughputBps: Long = 0L,
    val averageRttMs: Long = 0L,
    val packetLossRate: Float = 0.0f,
    val retryCount: Long = 0L,
    val queueSize: Int = 0,
    val averageLatencyMs: Long = 0L,
    val bandwidthUtilization: Float = 0.0f,
    val bleRssi: Int = -65,
    val wifiRssi: Int = -50,
    val fallbackCount: Long = 0L,
    val upgradeCount: Long = 0L,
    val downgradeCount: Long = 0L,
    val totalPacketsSentBle: Long = 0L,
    val totalPacketsSentWifi: Long = 0L,
    val packetTypeCounts: Map<String, Long> = emptyMap()
)
