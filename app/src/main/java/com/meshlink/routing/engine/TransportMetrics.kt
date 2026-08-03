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

    // Phase 5 Enhanced Transport Metrics
    private val _peakThroughputBps = AtomicLong(0)
    private val _rttMsSum = AtomicLong(0)
    private val _rttCount = AtomicLong(0)
    private val _sessionDurationMsSum = AtomicLong(0)
    private val _sessionCount = AtomicLong(0)
    private val _maxQueueCapacity = AtomicInteger(100)

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

    // Phase 5 Public Getters
    val peakThroughputBps: Long get() = _peakThroughputBps.get()

    val averageAckLatencyMs: Double
        get() {
            val count = _ackLatencyCount.get()
            if (count <= 0) return 0.0
            return _ackLatencyMsSum.get().toDouble() / count.toDouble()
        }

    val averageRttMs: Double
        get() {
            val count = _rttCount.get()
            if (count <= 0) return averageAckLatencyMs
            return _rttMsSum.get().toDouble() / count.toDouble()
        }

    val averageThroughputBps: Double
        get() {
            val durationMs = _mediaTransferDurationMs.get()
            val bytes = _totalBytesTransferred.get()
            if (durationMs <= 0) return 0.0
            return (bytes.toDouble() / (durationMs.toDouble() / 1000.0))
        }

    val packetSuccessRatePct: Float
        get() {
            val totalSent = _wifiPacketCount.get() + _blePacketCount.get()
            val totalFailures = _wifiFailures.get() + _bleFailures.get()
            if (totalSent <= 0) return 100f
            return (((totalSent - totalFailures).coerceAtLeast(0)).toFloat() / totalSent.toFloat() * 100f).coerceIn(0f, 100f)
        }

    val packetLossRatePct: Float
        get() = (100f - packetSuccessRatePct).coerceIn(0f, 100f)

    val averageSessionDurationMs: Double
        get() {
            val count = _sessionCount.get()
            if (count <= 0) return 0.0
            return _sessionDurationMsSum.get().toDouble() / count.toDouble()
        }

    val queueUtilizationPct: Float
        get() {
            val cap = _maxQueueCapacity.get()
            if (cap <= 0) return 0f
            return (_queueDepth.get().toFloat() / cap.toFloat() * 100f).coerceIn(0f, 100f)
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

    fun recordQueueDepth(depth: Int, maxCapacity: Int = 100) {
        _queueDepth.set(depth)
        if (maxCapacity > 0) _maxQueueCapacity.set(maxCapacity)
    }

    fun recordAckLatency(latencyMs: Long) {
        if (latencyMs >= 0) {
            _ackLatencyMsSum.addAndGet(latencyMs)
            _ackLatencyCount.incrementAndGet()
        }
    }

    // Phase 5 Metric Recorders
    fun recordThroughput(bps: Long) {
        if (bps > 0) {
            var currentPeak = _peakThroughputBps.get()
            while (bps > currentPeak) {
                if (_peakThroughputBps.compareAndSet(currentPeak, bps)) break
                currentPeak = _peakThroughputBps.get()
            }
        }
    }

    fun recordRtt(rttMs: Long) {
        if (rttMs >= 0) {
            _rttMsSum.addAndGet(rttMs)
            _rttCount.incrementAndGet()
        }
    }

    fun recordSessionDuration(durationMs: Long) {
        if (durationMs >= 0) {
            _sessionDurationMsSum.addAndGet(durationMs)
            _sessionCount.incrementAndGet()
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

        _peakThroughputBps.set(0)
        _rttMsSum.set(0)
        _rttCount.set(0)
        _sessionDurationMsSum.set(0)
        _sessionCount.set(0)
        _maxQueueCapacity.set(100)
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
            "avgRttMs" to averageRttMs,
            "peakThroughputBps" to peakThroughputBps,
            "avgThroughputBps" to averageThroughputBps,
            "packetSuccessRatePct" to packetSuccessRatePct,
            "packetLossRatePct" to packetLossRatePct,
            "queueUtilizationPct" to queueUtilizationPct,
            "avgSessionDurationMs" to averageSessionDurationMs,
            "transferEfficiencyPct" to transferEfficiencyPct,
            "totalBytes" to totalBytesTransferred,
            "mediaBytes" to mediaBytesTransferred,
            "avgMediaThroughputBps" to averageMediaThroughputBps
        )
    }
}
