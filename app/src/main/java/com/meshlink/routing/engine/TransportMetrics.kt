package com.meshlink.routing.engine

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe metrics collector for network operations.
 * Extended in Phase 2 for Sliding Window and Pipeline metrics.
 */
@Singleton
class TransportMetrics @Inject constructor() {

    // BLE Metrics
    private val _blePacketCount = AtomicLong(0)
    private val _blePacketsReceived = AtomicLong(0)
    private val _bleFailures = AtomicLong(0)
    private val _bleRetries = AtomicLong(0)

    // Wi-Fi Direct Metrics
    private val _wifiPacketCount = AtomicLong(0)
    private val _wifiPacketsReceived = AtomicLong(0)
    private val _wifiFailures = AtomicLong(0)
    private val _wifiRetries = AtomicLong(0)

    // Fallbacks and Total Volume
    private val _fallbackCount = AtomicLong(0)
    private val _retryCount = AtomicLong(0)
    private val _totalBytesTransferred = AtomicLong(0)

    // Media Specific Metrics
    private val _mediaBytesTransferred = AtomicLong(0)
    private val _mediaTransferDurationMs = AtomicLong(0)

    // Phase 2 Pipeline & Window Metrics
    private val _activeWindowSize = AtomicInteger(16)
    private val _retransmissionCount = AtomicLong(0)
    private val _workerUtilizationPct = AtomicLong(0) // Stored as scaled long percentage 0-100
    private val _queueDepth = AtomicInteger(0)
    private val _ackLatencyMsSum = AtomicLong(0)
    private val _ackLatencyCount = AtomicLong(0)

    // Public Getters (Backward Compatible)
    val blePacketCount: Long get() = _blePacketCount.get()
    val blePacketsReceived: Long get() = _blePacketsReceived.get()
    val bleFailures: Long get() = _bleFailures.get()
    val bleRetries: Long get() = _bleRetries.get()

    val wifiPacketCount: Long get() = _wifiPacketCount.get()
    val wifiPacketsReceived: Long get() = _wifiPacketsReceived.get()
    val wifiFailures: Long get() = _wifiFailures.get()
    val wifiRetries: Long get() = _wifiRetries.get()

    val fallbackCount: Long get() = _fallbackCount.get()
    val retryCount: Long get() = _retryCount.get()
    val totalBytesTransferred: Long get() = _totalBytesTransferred.get()

    val mediaBytesTransferred: Long get() = _mediaBytesTransferred.get()
    val mediaTransferDurationMs: Long get() = _mediaTransferDurationMs.get()

    // Phase 2 Public Getters
    val activeWindowSize: Int get() = _activeWindowSize.get()
    val retransmissions: Long get() = _retransmissionCount.get()
    val workerUtilizationPct: Float get() = _workerUtilizationPct.get().toFloat()
    val queueDepth: Int get() = _queueDepth.get()

    val averageAckLatencyMs: Double
        get() {
            val count = _ackLatencyCount.get()
            if (count <= 0) return 0.0
            return _ackLatencyMsSum.get().toDouble() / count.toDouble()
        }

    val transferEfficiencyPct: Float
        get() {
            val totalSent = _wifiPacketCount.get() + _blePacketCount.get()
            val retries = _retransmissionCount.get() + _retryCount.get()
            if (totalSent <= 0) return 100f
            return ((totalSent - retries).toFloat() / totalSent.toFloat() * 100f).coerceIn(0f, 100f)
        }

    val averageMediaThroughputBps: Double
        get() {
            val durationMs = _mediaTransferDurationMs.get()
            val bytes = _mediaBytesTransferred.get()
            if (durationMs <= 0) return 0.0
            return (bytes.toDouble() / (durationMs.toDouble() / 1000.0))
        }

    // Recording API
    fun recordBlePacket(bytes: Int = 0) {
        _blePacketCount.incrementAndGet()
        if (bytes > 0) _totalBytesTransferred.addAndGet(bytes.toLong())
    }

    fun recordBleRx(bytes: Int = 0) {
        _blePacketsReceived.incrementAndGet()
        if (bytes > 0) _totalBytesTransferred.addAndGet(bytes.toLong())
    }

    fun recordBleFailure() {
        _bleFailures.incrementAndGet()
    }

    fun recordBleRetry() {
        _bleRetries.incrementAndGet()
        _retryCount.incrementAndGet()
        _retransmissionCount.incrementAndGet()
    }

    fun recordWifiPacket(bytes: Int = 0) {
        _wifiPacketCount.incrementAndGet()
        if (bytes > 0) _totalBytesTransferred.addAndGet(bytes.toLong())
    }

    fun recordWifiRx(bytes: Int = 0) {
        _wifiPacketsReceived.incrementAndGet()
        if (bytes > 0) _totalBytesTransferred.addAndGet(bytes.toLong())
    }

    fun recordWifiFailure() {
        _wifiFailures.incrementAndGet()
    }

    fun recordWifiRetry() {
        _wifiRetries.incrementAndGet()
        _retryCount.incrementAndGet()
        _retransmissionCount.incrementAndGet()
    }

    fun recordFallback() {
        _fallbackCount.incrementAndGet()
    }

    fun recordRetry() {
        _retryCount.incrementAndGet()
        _retransmissionCount.incrementAndGet()
    }

    fun recordMediaTransfer(bytes: Long, durationMs: Long) {
        if (bytes > 0) _mediaBytesTransferred.addAndGet(bytes)
        if (durationMs > 0) _mediaTransferDurationMs.addAndGet(durationMs)
    }

    // Phase 2 Metric Recorders
    fun recordWindowSize(windowSize: Int) {
        _activeWindowSize.set(windowSize)
    }

    fun recordWorkerUtilization(activeWorkers: Int, totalWorkers: Int) {
        if (totalWorkers > 0) {
            val pct = (activeWorkers.toDouble() / totalWorkers.toDouble() * 100.0).toLong()
            _workerUtilizationPct.set(pct)
        }
    }

    fun recordQueueDepth(depth: Int) {
        _queueDepth.set(depth)
    }

    fun recordAckLatency(latencyMs: Long) {
        if (latencyMs >= 0) {
            _ackLatencyMsSum.addAndGet(latencyMs)
            _ackLatencyCount.incrementAndGet()
        }
    }

    fun reset() {
        _blePacketCount.set(0)
        _blePacketsReceived.set(0)
        _bleFailures.set(0)
        _bleRetries.set(0)

        _wifiPacketCount.set(0)
        _wifiPacketsReceived.set(0)
        _wifiFailures.set(0)
        _wifiRetries.set(0)

        _fallbackCount.set(0)
        _retryCount.set(0)
        _totalBytesTransferred.set(0)

        _mediaBytesTransferred.set(0)
        _mediaTransferDurationMs.set(0)

        _activeWindowSize.set(16)
        _retransmissionCount.set(0)
        _workerUtilizationPct.set(0)
        _queueDepth.set(0)
        _ackLatencyMsSum.set(0)
        _ackLatencyCount.set(0)
    }

    fun getSummary(): Map<String, Any> {
        return mapOf(
            "blePackets" to blePacketCount,
            "wifiPackets" to wifiPacketCount,
            "fallbacks" to fallbackCount,
            "retries" to retryCount,
            "retransmissions" to retransmissions,
            "activeWindowSize" to activeWindowSize,
            "workerUtilizationPct" to workerUtilizationPct,
            "queueDepth" to queueDepth,
            "avgAckLatencyMs" to averageAckLatencyMs,
            "transferEfficiencyPct" to transferEfficiencyPct,
            "totalBytes" to totalBytesTransferred,
            "mediaBytes" to mediaBytesTransferred,
            "avgMediaThroughputBps" to averageMediaThroughputBps
        )
    }
}
