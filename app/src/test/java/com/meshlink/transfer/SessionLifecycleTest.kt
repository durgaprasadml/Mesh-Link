package com.meshlink.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionLifecycleTest {

    private lateinit var registry: TransferSessionRegistry

    @Before
    fun setup() {
        registry = TransferSessionRegistry()
    }

    @Test
    fun `test state helper extensions`() {
        val sessionQueued = createSampleSession("s1", TransferState.QUEUED)
        val sessionSending = createSampleSession("s2", TransferState.SENDING)
        val sessionCompleted = createSampleSession("s3", TransferState.COMPLETED)
        val sessionCancelled = createSampleSession("s4", TransferState.CANCELLED)
        val sessionFailed = createSampleSession("s5", TransferState.FAILED)

        assertTrue(sessionQueued.isActive())
        assertFalse(sessionQueued.isTerminal())
        assertFalse(sessionQueued.requiresCleanup())

        assertTrue(sessionSending.isActive())
        assertFalse(sessionSending.isTerminal())
        assertFalse(sessionSending.requiresCleanup())

        assertFalse(sessionCompleted.isActive())
        assertTrue(sessionCompleted.isTerminal())
        assertFalse(sessionCompleted.requiresCleanup())

        assertFalse(sessionCancelled.isActive())
        assertTrue(sessionCancelled.isTerminal())
        assertTrue(sessionCancelled.requiresCleanup())

        assertFalse(sessionFailed.isActive())
        assertTrue(sessionFailed.isTerminal())
        assertTrue(sessionFailed.requiresCleanup())
    }

    @Test
    fun `test TransferSessionRegistry lifecycle registration and query`() {
        val s1 = createSampleSession("id-100", TransferState.SENDING)
        val s2 = createSampleSession("id-200", TransferState.RECEIVING)

        registry.registerSession(s1)
        registry.registerSession(s2)

        assertEquals(2, registry.getActiveCount())
        assertEquals(s1, registry.getSession("id-100"))
        assertTrue(registry.containsSession("id-200"))

        val unregistered = registry.unregisterSession("id-100")
        assertEquals(s1, unregistered)
        assertEquals(1, registry.getActiveCount())
        assertNull(registry.getSession("id-100"))

        registry.clearAll()
        assertEquals(0, registry.getActiveCount())
    }

    private fun createSampleSession(id: String, state: TransferState): TransferSession {
        return TransferSession(
            transferId = id,
            senderId = "sender-1",
            targetId = "target-1",
            fileName = "sample.png",
            mimeType = "image/png",
            totalBytes = 1024L,
            totalChunks = 10,
            direction = TransferDirection.OUTGOING,
            state = state
        )
    }
}
