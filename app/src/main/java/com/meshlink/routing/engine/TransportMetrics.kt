package com.meshlink.routing.engine

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportMetrics @Inject constructor() {

    private val _blePacketCount = AtomicLong(0)
    private val _wifiPacketCount = AtomicLong(0)
    private val _fallbackCount = AtomicLong(0)
    private val _retryCount = AtomicLong(0)
    private val _totalBytesTransferred = AtomicLong(0)

    val blePacketCount: Long get() = _blePacketCount.get()
    val wifiPacketCount: Long get() = _wifiPacketCount.get()
    val fallbackCount: Long get() = _fallbackCount.get()
    val retryCount: Long get() = _retryCount.get()
    val totalBytesTransferred: Long get() = _totalBytesTransferred.get()

    fun recordBlePacket(bytes: Int = 0) {
        _blePacketCount.incrementAndGet()
        if (bytes > 0) _totalBytesTransferred.addAndGet(bytes.toLong())
    }

    fun recordWifiPacket(bytes: Int = 0) {
        _wifiPacketCount.incrementAndGet()
        if (bytes > 0) _totalBytesTransferred.addAndGet(bytes.toLong())
    }

    fun recordFallback() {
        _fallbackCount.incrementAndGet()
    }

    fun recordRetry() {
        _retryCount.incrementAndGet()
    }

    fun reset() {
        _blePacketCount.set(0)
        _wifiPacketCount.set(0)
        _fallbackCount.set(0)
        _retryCount.set(0)
        _totalBytesTransferred.set(0)
    }

    fun getSummary(): Map<String, Any> {
        return mapOf(
            "blePackets" to blePacketCount,
            "wifiPackets" to wifiPacketCount,
            "fallbacks" to fallbackCount,
            "retries" to retryCount,
            "totalBytes" to totalBytesTransferred
        )
    }
}
