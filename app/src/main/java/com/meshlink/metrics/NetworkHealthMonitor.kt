package com.meshlink.metrics

import com.meshlink.routing.engine.TransportHealthMonitor
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkHealthReport(
    val uptimeMs: Long,
    val overallHealth: String,
    val bleConnected: Boolean,
    val bleRssi: Int,
    val bleMtu: Int,
    val bleAvgLatencyMs: Long,
    val bleReconnectCount: Long,
    val wifiConnected: Boolean,
    val wifiSocketState: String,
    val wifiThroughputBps: Double,
    val wifiRttMs: Long,
    val wifiReconnectCount: Long,
    val deliverySuccessRatePct: Float,
    val totalPacketsSent: Long,
    val totalPacketsFailed: Long
)

/**
 * Health monitor for tracking BLE and Wi-Fi Direct transport quality and stability.
 */
@Singleton
class NetworkHealthMonitor @Inject constructor(
    private val transportHealthMonitor: TransportHealthMonitor
) {
    fun generateReport(): NetworkHealthReport {
        val summary = transportHealthMonitor.getSummary()
        return NetworkHealthReport(
            uptimeMs = summary["uptimeMs"] as? Long ?: 0L,
            overallHealth = summary["overallHealth"] as? String ?: "UNKNOWN",
            bleConnected = summary["bleConnected"] as? Boolean ?: false,
            bleRssi = summary["bleRssi"] as? Int ?: 0,
            bleMtu = summary["bleMtu"] as? Int ?: 23,
            bleAvgLatencyMs = summary["bleAvgLatencyMs"] as? Long ?: 0L,
            bleReconnectCount = summary["bleReconnectCount"] as? Long ?: 0L,
            wifiConnected = summary["wifiConnected"] as? Boolean ?: false,
            wifiSocketState = summary["wifiSocketState"] as? String ?: "DISCONNECTED",
            wifiThroughputBps = summary["wifiThroughputBps"] as? Double ?: 0.0,
            wifiRttMs = summary["wifiRttMs"] as? Long ?: 0L,
            wifiReconnectCount = summary["wifiReconnectCount"] as? Long ?: 0L,
            deliverySuccessRatePct = summary["deliverySuccessRatePct"] as? Float ?: 100f,
            totalPacketsSent = summary["totalPacketsSent"] as? Long ?: 0L,
            totalPacketsFailed = summary["totalPacketsFailed"] as? Long ?: 0L
        )
    }
}
