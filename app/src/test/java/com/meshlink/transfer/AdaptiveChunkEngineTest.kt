package com.meshlink.transfer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdaptiveChunkEngineTest {

    private lateinit var engine: AdaptiveChunkEngine

    @Before
    fun setup() {
        engine = AdaptiveChunkEngine()
    }

    @Test
    fun `test BLE chunk size calculation stays within bounds`() {
        val size = engine.calculateChunkSize(
            transportType = TransportType.BLE,
            packetLossRate = 0.0f,
            averageRttMs = 50L
        )

        assertTrue("BLE chunk size should be >= MIN_BLE_CHUNK_BYTES", size >= AdaptiveChunkEngine.MIN_BLE_CHUNK_BYTES)
        assertTrue("BLE chunk size should be <= MAX_BLE_CHUNK_BYTES", size <= AdaptiveChunkEngine.MAX_BLE_CHUNK_BYTES)
    }

    @Test
    fun `test Wi-Fi Direct chunk size is significantly larger than BLE`() {
        val bleSize = engine.calculateChunkSize(TransportType.BLE)
        val wifiSize = engine.calculateChunkSize(TransportType.WIFI_DIRECT)

        assertTrue("Wi-Fi chunk size should be larger than BLE", wifiSize > bleSize * 10)
    }

    @Test
    fun `test high packet loss shrinks chunk size`() {
        val normalSize = engine.calculateChunkSize(TransportType.BLE, packetLossRate = 0.0f)
        val lossySize = engine.calculateChunkSize(TransportType.BLE, packetLossRate = 0.25f)

        assertTrue("High loss should reduce chunk size to prevent large retransmissions", lossySize < normalSize)
    }

    @Test
    fun `test CRC32 header framing and validation`() {
        val rawData = "Hello Mesh Link Media Subsystem!".toByteArray()
        val framed = engine.attachCrc32Header(rawData)

        assertEquals("Framed data should have 4 extra bytes for CRC32 header", rawData.size + 4, framed.size)

        val unFramed = engine.validateAndStripCrc32Header(framed)
        assertNotNull("Valid CRC32 should return payload", unFramed)
        assertArrayEquals("Payload bytes must match original input", rawData, unFramed)
    }

    @Test
    fun `test corrupt CRC32 header returns null`() {
        val rawData = "Corrupt test data payload".toByteArray()
        val framed = engine.attachCrc32Header(rawData)

        // Corrupt payload byte
        framed[framed.size - 1] = (framed[framed.size - 1] + 1).toByte()

        val result = engine.validateAndStripCrc32Header(framed)
        assertNull("Corrupt payload must fail CRC32 verification and return null", result)
    }
}
