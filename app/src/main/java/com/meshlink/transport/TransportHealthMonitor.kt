package com.meshlink.transport

import com.meshlink.domain.model.RouteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class TransportHealthMetrics(
    val routeType: RouteType,
    val isAvailable: Boolean = false,
    val averageLatencyMs: Long = 0L,
    val packetLossRate: Float = 0.0f,
    val reconnectCount: Int = 0,
    val signalQualityRssi: Int = -100,
    val totalSent: Long = 0L,
    val totalFailed: Long = 0L,
    val confidenceScore: Float = 1.0f // 0.0 to 1.0 smooth degradation
)

@Singleton
class TransportHealthMonitor @Inject constructor() {

    private val metricsMap = ConcurrentHashMap<RouteType, TransportHealthMetrics>().apply {
        put(RouteType.BLE, TransportHealthMetrics(RouteType.BLE, isAvailable = true, signalQualityRssi = -70))
        put(RouteType.WIFI_DIRECT, TransportHealthMetrics(RouteType.WIFI_DIRECT, isAvailable = false, signalQualityRssi = -100))
        put(RouteType.HYBRID, TransportHealthMetrics(RouteType.HYBRID, isAvailable = true, signalQualityRssi = -70))
    }

    private val reconnectCounters = ConcurrentHashMap<RouteType, AtomicInteger>()
    private val sentCounters = ConcurrentHashMap<RouteType, AtomicLong>()
    private val failCounters = ConcurrentHashMap<RouteType, AtomicLong>()

    private val _transportHealth = MutableStateFlow<Map<RouteType, TransportHealthMetrics>>(metricsMap.toMap())
    val transportHealth: StateFlow<Map<RouteType, TransportHealthMetrics>> = _transportHealth.asStateFlow()

    fun updateAvailability(routeType: RouteType, available: Boolean) {
        val current = metricsMap[routeType] ?: TransportHealthMetrics(routeType)
        val updated = current.copy(isAvailable = available)
        metricsMap[routeType] = updated
        recomputeConfidence(routeType)
    }

    fun recordTransmission(routeType: RouteType, latencyMs: Long, success: Boolean, rssi: Int = -70) {
        val current = metricsMap[routeType] ?: TransportHealthMetrics(routeType)
        val sent = sentCounters.computeIfAbsent(routeType) { AtomicLong(0) }.incrementAndGet()
        val failed = if (!success) {
            failCounters.computeIfAbsent(routeType) { AtomicLong(0) }.incrementAndGet()
        } else {
            failCounters.computeIfAbsent(routeType) { AtomicLong(0) }.get()
        }

        val lossRate = if (sent > 0) failed.toFloat() / sent else 0.0f
        val newLatency = if (current.averageLatencyMs == 0L) latencyMs else (0.8f * current.averageLatencyMs + 0.2f * latencyMs).toLong()

        val updated = current.copy(
            averageLatencyMs = newLatency,
            packetLossRate = lossRate,
            signalQualityRssi = rssi,
            totalSent = sent,
            totalFailed = failed
        )
        metricsMap[routeType] = updated
        recomputeConfidence(routeType)
    }

    fun recordReconnect(routeType: RouteType) {
        val count = reconnectCounters.computeIfAbsent(routeType) { AtomicInteger(0) }.incrementAndGet()
        val current = metricsMap[routeType] ?: TransportHealthMetrics(routeType)
        metricsMap[routeType] = current.copy(reconnectCount = count)
        recomputeConfidence(routeType)
    }

    private fun recomputeConfidence(routeType: RouteType) {
        val current = metricsMap[routeType] ?: return
        if (!current.isAvailable) {
            metricsMap[routeType] = current.copy(confidenceScore = 0.0f)
            publish()
            return
        }

        var score = 1.0f
        // Deduct for packet loss
        score -= current.packetLossRate * 0.5f

        // Deduct for high latency (>200ms)
        if (current.averageLatencyMs > 200L) {
            val latencyPenalty = minOf(0.3f, (current.averageLatencyMs - 200L) / 1000.0f)
            score -= latencyPenalty
        }

        // Deduct for weak signal (< -85 dBm)
        if (current.signalQualityRssi < -85) {
            val rssiPenalty = minOf(0.2f, (-85 - current.signalQualityRssi) * 0.01f)
            score -= rssiPenalty
        }

        // Deduct for high reconnect count
        if (current.reconnectCount > 3) {
            score -= minOf(0.2f, current.reconnectCount * 0.03f)
        }

        val finalConfidence = score.coerceIn(0.0f, 1.0f)
        metricsMap[routeType] = current.copy(confidenceScore = finalConfidence)
        publish()
    }

    private fun publish() {
        _transportHealth.update { metricsMap.toMap() }
    }

    fun getConfidence(routeType: RouteType): Float {
        return metricsMap[routeType]?.confidenceScore ?: 0.0f
    }
}
