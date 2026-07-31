package com.meshlink.transfer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ResumeManagerTest {

    private lateinit var resumeManager: ResumeManager

    @Before
    fun setup() {
        resumeManager = ResumeManager()
    }

    @Test
    fun `test resume state tracks ACKed chunks and remaining missing chunks`() {
        val transferId = "res-123"
        resumeManager.initOrRecoverState(transferId, "hash123", totalChunks = 5, existingAckedIndices = setOf(0, 1))

        var missing = resumeManager.getMissingChunks(transferId, 5)
        assertEquals("Missing chunks should be [2, 3, 4]", listOf(2, 3, 4), missing)

        resumeManager.recordChunkAck(transferId, 2)
        missing = resumeManager.getMissingChunks(transferId, 5)
        assertEquals("Missing chunks should now be [3, 4]", listOf(3, 4), missing)
    }

    @Test
    fun `test metadata mismatch invalidates resume state`() {
        val transferId = "res-456"
        resumeManager.initOrRecoverState(transferId, "original_hash", totalChunks = 10, existingAckedIndices = setOf(0, 1, 2))

        val isValid = resumeManager.isResumeMetadataValid(transferId, "new_different_hash")
        assertFalse("Metadata hash mismatch must invalidate resume state to prevent file corruption", isValid)

        val stateAfter = resumeManager.getResumeState(transferId)
        assertNull("State should be cleared after metadata mismatch", stateAfter)
    }
}
