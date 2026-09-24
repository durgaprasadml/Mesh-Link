package com.meshlink.transfer

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized, configurable transfer policies and parameters.
 * Eliminates magic numbers across transfer pipeline components.
 */
@Singleton
class TransferConfiguration @Inject constructor() {

    var wifiWindowSize: Int = 16
    // Increased from 4 → 12: a 23-chunk audio file now uses ~2 window-advance rounds
    // instead of 6, dramatically reducing per-file transfer time over BLE.
    var bleWindowSize: Int = 12
    var workerCount: Int = 4

    var wifiAckTimeoutMs: Long = 2000L
    // Reduced from 5000 → 2500: a 2-hop BLE mesh ACK round-trip is typically 200–800ms,
    // so 2500ms gives 3–12× headroom without triggering premature retry storms.
    var bleAckTimeoutMs: Long = 2500L
    var retryLimit: Int = 3

    var queueCapacity: Int = 64
    // Aligned with new bleWindowSize (was 8 vs window 4 — batch was larger than window).
    var dispatchBatchSize: Int = 12

    // Small-file fast-path: files with ≤ smallFileChunkThreshold chunks bypass window
    // gating entirely — all chunks are dispatched in one pass.  This covers any audio
    // file up to ~18 KB (100 × 180 B), which includes a typical 2–5 second voice note.
    var smallFileChunkThreshold: Int = 100
    var bleSmallFileWindowSize: Int = 256   // Large enough to cover any small file
    var bleSmallFileAckTimeoutMs: Long = 2000L

    fun getWindowSize(transportType: TransportType): Int {
        return when (transportType) {
            TransportType.WIFI_DIRECT -> wifiWindowSize
            TransportType.BLE -> bleWindowSize
            else -> bleWindowSize
        }
    }

    /**
     * Returns the sliding-window size for the given transport, applying the small-file
     * fast-path when [totalChunks] is below [smallFileChunkThreshold].  For small files
     * (e.g. a short voice note ≤ ~18 KB) the window covers ALL chunks, so they are all
     * dispatched in a single pass without waiting for intermediate ACKs to advance the window.
     */
    fun getWindowSize(transportType: TransportType, totalChunks: Int): Int {
        if (transportType == TransportType.BLE && totalChunks <= smallFileChunkThreshold) {
            return bleSmallFileWindowSize
        }
        return getWindowSize(transportType)
    }

    fun getAckTimeoutMs(transportType: TransportType): Long {
        return when (transportType) {
            TransportType.WIFI_DIRECT -> wifiAckTimeoutMs
            TransportType.BLE -> bleAckTimeoutMs
            else -> bleAckTimeoutMs
        }
    }

    /**
     * Returns the ACK timeout appropriate for the given transport and file size.
     * Small files use a tighter timeout since they should complete quickly.
     */
    fun getAckTimeoutMs(transportType: TransportType, totalChunks: Int): Long {
        if (transportType == TransportType.BLE && totalChunks <= smallFileChunkThreshold) {
            return bleSmallFileAckTimeoutMs
        }
        return getAckTimeoutMs(transportType)
    }
}
