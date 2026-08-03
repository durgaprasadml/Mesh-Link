package com.meshlink.routing.engine

import com.meshlink.domain.transport.TransportHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detailed passive health state for BLE transport.
 */
data class BleHealthMetrics(
    val isConnected: Boolean = false,
    val rssi: Int? = null,
    val mtu: Int = 23,
    val packetSuccessRate: Float = 100.0f,
    val averageLatencyMs: Long = 0L,
    val lastActivityTimestamp: Long = 0L
)

/**
 * Detailed passive health state for Wi-Fi Direct transport.
 */
data class WifiHealthMetrics(
    val isConnected: Boolean = false,
    val socketState: String = "DISCONNECTED",
    val estimatedThroughputBps: Double = 0.0,
    val packetSuccessRate: Float = 100.0f,
    val rttMs: Long = 0L,
    val lastActivityTimestamp: Long = 0L
)

/**
 * Unified health status container exposed as a read-only StateFlow.
 */
data class TransportHealthStatus(
    val bleMetrics: BleHealthMetrics = BleHealthMetrics(),
    val wifiMetrics: WifiHealthMetrics = WifiHealthMetrics(),
    val overallHealth: TransportHealth = TransportHealth.AVAILABLE
)

/**
 * Diagnostics model for backward compatibility with ProductionHardeningValidator.
 */
data class TransportHealthDiagnostics(
    val activeBlePeers: Int = 0,
    val activeWifiPeers: Int = 0,
    val bleStatus: TransportHealth = TransportHealth.DISCONNECTED,
    val wifiStatus: TransportHealth = TransportHealth.DISCONNECTED,
    val totalPacketsSent: Long = 0L,
    val totalPacketsFailed: Long = 0L,
    val totalRetries: Long = 0L,
    val totalFallbacks: Long = 0L,
    val deliverySuccessRatePercentage: Float = 100.0f
)

/**
 * Lightweight, singleton service tracking passive transport health without active heartbeat probes.
 */
@Singleton
class TransportHealthMonitor @Inject constructor() {

    private val packetsSentCount = AtomicLong(0L)
    private val packetsFailedCount = AtomicLong(0L)
    private val retriesCount = AtomicLong(0L)
    private val fallbacksCount = AtomicLong(0L)
    private val activeBlePeersCount = AtomicInteger(0)
    private val activeWifiPeersCount = AtomicInteger(0)

    private val _healthStatus = MutableStateFlow(TransportHealthStatus())
    val healthStatus: StateFlow<TransportHealthStatus> = _healthStatus.asStateFlow()

    private val _diagnostics = MutableStateFlow(TransportHealthDiagnostics())
    val diagnostics: StateFlow<TransportHealthDiagnostics> = _diagnostics.asStateFlow()

    fun updateBleState(status: TransportHealth, activePeers: Int, rssi: Int? = null, mtu: Int = 23) {
        activeBlePeersCount.set(activePeers)
        val isConnected = status == TransportHealth.CONNECTED || activePeers > 0
        _healthStatus.value = _healthStatus.value.copy(
            bleMetrics = _healthStatus.value.bleMetrics.copy(
                isConnected = isConnected,
                rssi = rssi ?: _healthStatus.value.bleMetrics.rssi,
                mtu = mtu,
                lastActivityTimestamp = System.currentTimeMillis()
            ),
            overallHealth = evaluateOverallHealth(status, _diagnostics.value.wifiStatus)
        )
        updateDiagnostics { copy(bleStatus = status, activeBlePeers = activePeers) }
    }

    fun updateWifiState(status: TransportHealth, activePeers: Int, socketState: String = "CONNECTED", estimatedThroughputBps: Double = 0.0) {
        activeWifiPeersCount.set(activePeers)
        val isConnected = status == TransportHealth.CONNECTED || activePeers > 0
        _healthStatus.value = _healthStatus.value.copy(
            wifiMetrics = _healthStatus.value.wifiMetrics.copy(
                isConnected = isConnected,
                socketState = if (isConnected) socketState else "DISCONNECTED",
                estimatedThroughputBps = if (isConnected) estimatedThroughputBps else 0.0,
                lastActivityTimestamp = System.currentTimeMillis()
            ),
            overallHealth = evaluateOverallHealth(_diagnostics.value.bleStatus, status)
        )
        updateDiagnostics { copy(wifiStatus = status, activeWifiPeers = activePeers) }
    }

    fun recordBleTxResult(success: Boolean, latencyMs: Long = 0L) {
        val current = _healthStatus.value.bleMetrics
        val newCount = if (success) current.lastActivityTimestamp + 1 else current.lastActivityTimestamp
        val updatedRate = if (success) (current.packetSuccessRate * 0.95f + 5.0f).coerceAtMost(100f) else (current.packetSuccessRate * 0.95f).coerceAtLeast(0f)
        _healthStatus.value = _healthStatus.value.copy(
            bleMetrics = current.copy(
                packetSuccessRate = updatedRate,
                averageLatencyMs = if (latencyMs > 0) (current.averageLatencyMs * 0.8 + latencyMs * 0.2).toLong() else current.averageLatencyMs,
                lastActivityTimestamp = System.currentTimeMillis()
            )
        )
        if (success) recordPacketSent() else recordPacketFailed()
    }

    fun recordWifiTxResult(success: Boolean, rttMs: Long = 0L, bytes: Int = 0) {
        val current = _healthStatus.value.wifiMetrics
        val updatedRate = if (success) (current.packetSuccessRate * 0.95f + 5.0f).coerceAtMost(100f) else (current.packetSuccessRate * 0.95f).coerceAtLeast(0f)
        _healthStatus.value = _healthStatus.value.copy(
            wifiMetrics = current.copy(
                packetSuccessRate = updatedRate,
                rttMs = if (rttMs > 0) (current.rttMs * 0.8 + rttMs * 0.2).toLong() else current.rttMs,
                lastActivityTimestamp = System.currentTimeMillis()
            )
        )
        if (success) recordPacketSent() else recordPacketFailed()
    }

    fun recordPacketSent() {
        val sent = packetsSentCount.incrementAndGet()
        recalculateSuccessRate(sent, packetsFailedCount.get())
    }

    fun recordPacketFailed() {
        val failed = packetsFailedCount.incrementAndGet()
        recalculateSuccessRate(packetsSentCount.get(), failed)
    }

    fun recordRetry() {
        val retries = retriesCount.incrementAndGet()
        updateDiagnostics { copy(totalRetries = retries) }
    }

    fun recordFallback() {
        val fallbacks = fallbacksCount.incrementAndGet()
        updateDiagnostics { copy(totalFallbacks = fallbacks) }
    }

    private fun recalculateSuccessRate(sent: Long, failed: Long) {
        val total = sent + failed
        val rate = if (total > 0) (sent.toFloat() / total.toFloat()) * 100f else 100f
        updateDiagnostics {
            copy(
                totalPacketsSent = sent,
                totalPacketsFailed = failed,
                deliverySuccessRatePercentage = rate
            )
        }
    }

    private fun evaluateOverallHealth(bleStatus: TransportHealth, wifiStatus: TransportHealth): TransportHealth {
        return when {
            wifiStatus == TransportHealth.CONNECTED || bleStatus == TransportHealth.CONNECTED -> TransportHealth.CONNECTED
            wifiStatus == TransportHealth.CONNECTING || bleStatus == TransportHealth.CONNECTING -> TransportHealth.CONNECTING
            wifiStatus == TransportHealth.AVAILABLE || bleStatus == TransportHealth.AVAILABLE -> TransportHealth.AVAILABLE
            else -> TransportHealth.DISCONNECTED
        }
    }

    private inline fun updateDiagnostics(update: TransportHealthDiagnostics.() -> TransportHealthDiagnostics) {
        _diagnostics.value = _diagnostics.value.update()
    }
}
