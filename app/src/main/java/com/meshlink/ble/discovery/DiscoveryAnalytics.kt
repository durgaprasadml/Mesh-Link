package com.meshlink.ble.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiscoveryMetrics(
    val totalPeersDiscovered: Int = 0,
    val averageRssi: Int = -100,
    val duplicateSuppressedCount: Int = 0,
    val failedConnectionsCount: Int = 0,
    val scanCyclesCompleted: Int = 0,
    val activeConnections: Int = 0
)

/**
 * Collects and exposes real-time statistics about the discovery engine.
 */
class DiscoveryAnalytics {

    private val _metrics = MutableStateFlow(DiscoveryMetrics())
    val metrics: StateFlow<DiscoveryMetrics> = _metrics.asStateFlow()
    
    private var rssiSum = 0L
    private var rssiSamples = 0L

    fun recordPeerDiscovered() {
        _metrics.update { it.copy(totalPeersDiscovered = it.totalPeersDiscovered + 1) }
    }

    fun recordRssi(rssi: Int) {
        rssiSum += rssi
        rssiSamples++
        val avg = (rssiSum / rssiSamples).toInt()
        _metrics.update { it.copy(averageRssi = avg) }
    }

    fun recordDuplicateSuppressed() {
        _metrics.update { it.copy(duplicateSuppressedCount = it.duplicateSuppressedCount + 1) }
    }

    fun recordConnectionFailed() {
        _metrics.update { it.copy(failedConnectionsCount = it.failedConnectionsCount + 1) }
    }
    
    fun recordScanCycle() {
        _metrics.update { it.copy(scanCyclesCompleted = it.scanCyclesCompleted + 1) }
    }
    
    fun updateActiveConnections(count: Int) {
        _metrics.update { it.copy(activeConnections = count) }
    }
}
