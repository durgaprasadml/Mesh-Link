package com.meshlink.transfer

import java.nio.ByteBuffer
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Production Adaptive Chunk Engine for Mesh Link Media Transfers.
 *
 * Dynamically computes chunk sizes based on:
 * - Current transport (BLE vs Wi-Fi Direct)
 * - Observed Round-Trip Time (RTT) and packet loss rate
 * - Available throughput & memory pressure
 * - Chunk retransmission count
 *
 * Also injects and validates a 4-byte CRC32 header into every raw chunk
 * to ensure per-chunk integrity verification before Base64 encoding or file assembly.
 */
@Singleton
class AdaptiveChunkEngine @Inject constructor() {

    companion object {
        // BLE chunk bounds (raw bytes before Base64 encoding)
        const val MIN_BLE_CHUNK_BYTES = 180      // ~240B B64 (safe for 256-byte ATT MTU)
        const val TARGET_BLE_CHUNK_BYTES = 512    // ~684B B64 (safe for 512-byte ATT MTU)
        const val MAX_BLE_CHUNK_BYTES = 1024     // 1 KB raw (high-quality BLE link)

        // Wi-Fi Direct chunk bounds (raw bytes before Base64 encoding)
        const val MIN_WIFI_CHUNK_BYTES = 16 * 1024  // 16 KB
        const val TARGET_WIFI_CHUNK_BYTES = 32 * 1024 // 32 KB
        const val MAX_WIFI_CHUNK_BYTES = 64 * 1024  // 64 KB

        const val CRC32_HEADER_SIZE = 4 // 4 bytes uint32 CRC32 header
    }

    /**
     * Calculates the optimal dynamic raw chunk size (in bytes) based on network conditions.
     */
    fun calculateChunkSize(
        transportType: TransportType,
        packetLossRate: Float = 0f,
        averageRttMs: Long = 100L,
        retryCount: Int = 0,
        memoryPressureHigh: Boolean = false
    ): Int {
        val baseSize = when (transportType) {
            TransportType.WIFI_DIRECT -> TARGET_WIFI_CHUNK_BYTES
            TransportType.BLE -> TARGET_BLE_CHUNK_BYTES
            TransportType.HYBRID -> TARGET_WIFI_CHUNK_BYTES
            else -> TARGET_BLE_CHUNK_BYTES
        }

        val minSize = when (transportType) {
            TransportType.WIFI_DIRECT -> MIN_WIFI_CHUNK_BYTES
            TransportType.BLE -> MIN_BLE_CHUNK_BYTES
            TransportType.HYBRID -> MIN_WIFI_CHUNK_BYTES
            else -> MIN_BLE_CHUNK_BYTES
        }

        val maxSize = when (transportType) {
            TransportType.WIFI_DIRECT -> MAX_WIFI_CHUNK_BYTES
            TransportType.BLE -> MAX_BLE_CHUNK_BYTES
            TransportType.HYBRID -> MAX_WIFI_CHUNK_BYTES
            else -> MAX_BLE_CHUNK_BYTES
        }

        var adaptedSize = baseSize.toDouble()

        // 1. Loss penalty: decrease size on high loss to avoid retransmitting huge chunks
        if (packetLossRate > 0.20f) {
            adaptedSize *= 0.5
        } else if (packetLossRate > 0.05f) {
            adaptedSize *= 0.75
        } else if (packetLossRate < 0.01f && averageRttMs < 80L) {
            adaptedSize *= 1.25 // Smooth link boost
        }

        // 2. High RTT penalty
        if (averageRttMs > 400L) {
            adaptedSize *= 0.7
        }

        // 3. Retry count penalty: shrink chunk if retries are occurring
        if (retryCount > 2) {
            adaptedSize *= 0.6
        }

        // 4. Memory pressure protection
        if (memoryPressureHigh) {
            adaptedSize = minOf(adaptedSize, minSize.toDouble())
        }

        return max(minSize.toDouble(), min(maxSize.toDouble(), adaptedSize)).toInt()
    }

    /**
     * Attaches a 4-byte CRC32 header to raw chunk payload bytes.
     * Returns a new ByteArray containing [4-byte CRC32 BigEndian] + [rawChunkBytes].
     */
    fun attachCrc32Header(rawChunkBytes: ByteArray): ByteArray {
        val crc = CRC32()
        crc.update(rawChunkBytes)
        val crcValue = crc.value.toInt()

        val output = ByteArray(CRC32_HEADER_SIZE + rawChunkBytes.size)
        ByteBuffer.wrap(output).putInt(0, crcValue)
        System.arraycopy(rawChunkBytes, 0, output, CRC32_HEADER_SIZE, rawChunkBytes.size)
        return output
    }

    /**
     * Validates and strips the 4-byte CRC32 header from an incoming raw chunk packet payload.
     * Returns the payload ByteArray without header if valid, or null if CRC mismatch or corrupt length.
     */
    fun validateAndStripCrc32Header(framedChunkBytes: ByteArray): ByteArray? {
        if (framedChunkBytes.size <= CRC32_HEADER_SIZE) return null

        val expectedCrc = ByteBuffer.wrap(framedChunkBytes, 0, CRC32_HEADER_SIZE).int
        val payload = ByteArray(framedChunkBytes.size - CRC32_HEADER_SIZE)
        System.arraycopy(framedChunkBytes, CRC32_HEADER_SIZE, payload, 0, payload.size)

        val crc = CRC32()
        crc.update(payload)
        val computedCrc = crc.value.toInt()

        return if (expectedCrc == computedCrc) payload else null
    }
}
