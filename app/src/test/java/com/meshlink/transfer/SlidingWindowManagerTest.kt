package com.meshlink.transfer

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SlidingWindowManagerTest {

    private lateinit var config: TransferConfiguration
    private lateinit var runtimeStateRegistry: TransferRuntimeStateRegistry
    private lateinit var slidingWindowManager: SlidingWindowManager

    @Before
    fun setUp() {
        config = TransferConfiguration()
        runtimeStateRegistry = TransferRuntimeStateRegistry()
        slidingWindowManager = SlidingWindowManager(config, runtimeStateRegistry)
    }

    @Test
    fun `initializeSessionWindow sets window size based on transport`() {
        val wifiSize = slidingWindowManager.initializeSessionWindow("transfer_wifi", TransportType.WIFI_DIRECT, 100)
        val bleSize = slidingWindowManager.initializeSessionWindow("transfer_ble", TransportType.BLE, 100)

        assertEquals(16, wifiSize)
        assertEquals(4, bleSize)
        assertEquals(0, slidingWindowManager.getBase("transfer_wifi"))
    }

    @Test
    fun `canSend correctly identifies sendable chunk range`() {
        slidingWindowManager.initializeSessionWindow("transfer_1", TransportType.WIFI_DIRECT, 20)

        assertTrue(slidingWindowManager.canSend("transfer_1", 0))
        assertTrue(slidingWindowManager.canSend("transfer_1", 15))
        assertFalse(slidingWindowManager.canSend("transfer_1", 16))
        assertFalse(slidingWindowManager.canSend("transfer_1", 20))
    }

    @Test
    fun `getNextSendableIndices returns indices up to window boundary`() {
        slidingWindowManager.initializeSessionWindow("transfer_1", TransportType.WIFI_DIRECT, 20)

        val sendable = slidingWindowManager.getNextSendableIndices("transfer_1", maxBatchSize = 10)
        assertEquals(10, sendable.size)
        assertEquals(0, sendable.first())
        assertEquals(9, sendable.last())
    }

    @Test
    fun `onAckReceived advances window base correctly`() {
        slidingWindowManager.initializeSessionWindow("transfer_1", TransportType.WIFI_DIRECT, 20)

        // Receive ACK for chunk 0
        val result0 = slidingWindowManager.onAckReceived("transfer_1", 0)
        assertEquals(1, result0.advancedCount)
        assertEquals(0, result0.oldBase)
        assertEquals(1, result0.newBase)
        assertFalse(result0.isComplete)

        // Receive out-of-order ACK for chunk 2 (base shouldn't advance past 1 until chunk 1 is ACKed)
        val result2 = slidingWindowManager.onAckReceived("transfer_1", 2)
        assertEquals(0, result2.advancedCount)
        assertEquals(1, result2.newBase)

        // Receive ACK for chunk 1 (base should advance to 3 because 2 was already ACKed!)
        val result1 = slidingWindowManager.onAckReceived("transfer_1", 1)
        assertEquals(2, result1.advancedCount)
        assertEquals(3, result1.newBase)
    }

    @Test
    fun `onCumulativeAck advances window up to specified index`() {
        slidingWindowManager.initializeSessionWindow("transfer_cum", TransportType.WIFI_DIRECT, 10)

        val result = slidingWindowManager.onCumulativeAck("transfer_cum", 4)
        assertEquals(5, result.advancedCount)
        assertEquals(5, result.newBase)
        assertFalse(result.isComplete)
    }

    @Test
    fun `window completion returns isComplete true when all chunks ACKed`() {
        slidingWindowManager.initializeSessionWindow("transfer_short", TransportType.BLE, 2)

        slidingWindowManager.onAckReceived("transfer_short", 0)
        val finalResult = slidingWindowManager.onAckReceived("transfer_short", 1)

        assertTrue(finalResult.isComplete)
        assertEquals(2, finalResult.newBase)
    }
}
