package com.meshlink.transfer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TransferQueueSchedulerTest {

    private lateinit var scheduler: TransferQueueScheduler

    @Before
    fun setup() {
        scheduler = TransferQueueScheduler()
    }

    @Test
    fun `test critical priority SOS transfer bypasses queue limit`() {
        val sosSession = TransferSession(
            transferId = "sos-1",
            senderId = "node1",
            targetId = "node2",
            fileName = "sos.jpg",
            mimeType = "image/jpeg",
            totalBytes = 1000L,
            totalChunks = 2,
            direction = TransferDirection.OUTGOING,
            priority = TransferPriority.CRITICAL,
            state = TransferState.SENDING
        )

        scheduler.addSession(sosSession)
        assertTrue("CRITICAL SOS transfer must always be allowed to send", scheduler.canSendNextChunk("sos-1"))
    }

    @Test
    fun `test sliding window expansion and contraction`() {
        val session = TransferSession(
            transferId = "t1",
            senderId = "s1",
            targetId = "t1",
            fileName = "img.jpg",
            mimeType = "image/jpeg",
            totalBytes = 50000L,
            totalChunks = 10,
            direction = TransferDirection.OUTGOING,
            transportUsed = TransportType.WIFI_DIRECT,
            state = TransferState.SENDING
        )

        scheduler.addSession(session)
        val initialWindow = scheduler.getSlidingWindowSize("t1")

        // Record ACK arrival -> window expands
        scheduler.recordAckArrival("t1")
        val expandedWindow = scheduler.getSlidingWindowSize("t1")
        assertTrue("ACK arrival should expand sliding window", expandedWindow >= initialWindow)

        // Increment retry -> window contracts
        scheduler.incrementRetry("t1")
        val contractedWindow = scheduler.getSlidingWindowSize("t1")
        assertTrue("Retry / Loss should contract sliding window", contractedWindow < expandedWindow)
    }
}
