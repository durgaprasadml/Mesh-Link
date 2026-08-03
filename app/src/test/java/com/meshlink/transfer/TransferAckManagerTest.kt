package com.meshlink.transfer

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.routing.engine.TransportMetrics
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TransferAckManagerTest {

    private lateinit var config: TransferConfiguration
    private lateinit var runtimeStateRegistry: TransferRuntimeStateRegistry
    private lateinit var slidingWindowManager: SlidingWindowManager
    private lateinit var metrics: TransportMetrics
    private lateinit var diagnostics: TransportDiagnostics
    private lateinit var ackManager: TransferAckManager

    @Before
    fun setUp() {
        config = TransferConfiguration()
        runtimeStateRegistry = TransferRuntimeStateRegistry()
        slidingWindowManager = SlidingWindowManager(config, runtimeStateRegistry)
        metrics = TransportMetrics()
        diagnostics = TransportDiagnostics()
        ackManager = TransferAckManager(slidingWindowManager, runtimeStateRegistry, metrics, diagnostics)
    }

    @Test
    fun `processAck updates window base and returns correct process result`() {
        val transferId = "ack_test_1"
        slidingWindowManager.initializeSessionWindow(transferId, TransportType.WIFI_DIRECT, 10)

        val ackPacket = MeshPacket(
            senderId = "userB",
            targetId = "userA",
            transferId = transferId,
            payload = "0",
            type = PacketType.MEDIA_ACK,
            chunkIndex = 0,
            totalChunks = 10
        )

        val result = ackManager.processAck(ackPacket, 10)

        assertNotNull(result)
        assertFalse(result!!.isDuplicateOrStale)
        assertEquals(1, result.windowAdvancedCount)
        assertEquals(1, result.newWindowBase)
        assertFalse(result.isTransferComplete)
    }

    @Test
    fun `processAck identifies duplicate ACKs`() {
        val transferId = "ack_dup_test"
        slidingWindowManager.initializeSessionWindow(transferId, TransportType.WIFI_DIRECT, 10)

        val ackPacket = MeshPacket(
            senderId = "userB",
            targetId = "userA",
            transferId = transferId,
            payload = "0",
            type = PacketType.MEDIA_ACK,
            chunkIndex = 0,
            totalChunks = 10
        )

        // First ACK
        ackManager.processAck(ackPacket, 10)

        // Duplicate ACK
        val dupResult = ackManager.processAck(ackPacket, 10)

        assertNotNull(dupResult)
        assertTrue(dupResult!!.isDuplicateOrStale)
        assertEquals(0, dupResult.windowAdvancedCount)
    }
}
