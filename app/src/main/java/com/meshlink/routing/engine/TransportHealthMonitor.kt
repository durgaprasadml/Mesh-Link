package com.meshlink.routing.engine

import com.meshlink.domain.transport.TransportHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class TransportHealthMonitor @Inject constructor() {

    private val packetsSentCount = AtomicLong(0L)
    private val packetsFailedCount = AtomicLong(0L)
    private val retriesCount = AtomicLong(0L)
    private val fallbacksCount = AtomicLong(0L)
    private val activeBlePeersCount = AtomicInteger(0)
    private val activeWifiPeersCount = AtomicInteger(0)

    private val _diagnostics = MutableStateFlow(TransportHealthDiagnostics())
    val diagnostics: StateFlow<TransportHealthDiagnostics> = _diagnostics.asStateFlow()

    fun updateBleState(status: TransportHealth, activePeers: Int) {
        activeBlePeersCount.set(activePeers)
        updateDiagnostics { copy(bleStatus = status, activeBlePeers = activePeers) }
    }

    fun updateWifiState(status: TransportHealth, activePeers: Int) {
        activeWifiPeersCount.set(activePeers)
        updateDiagnostics { copy(wifiStatus = status, activeWifiPeers = activePeers) }
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

    private inline fun updateDiagnostics(update: TransportHealthDiagnostics.() -> TransportHealthDiagnostics) {
        _diagnostics.value = _diagnostics.value.update()
    }
}
