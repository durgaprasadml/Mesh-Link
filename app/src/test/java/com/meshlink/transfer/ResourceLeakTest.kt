package com.meshlink.transfer

import com.meshlink.routing.engine.TransportDiagnostics
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResourceLeakTest {

    private lateinit var resourceManager: TransferResourceManager
    private lateinit var diagnostics: TransportDiagnostics

    @Before
    fun setup() {
        diagnostics = TransportDiagnostics()
        resourceManager = TransferResourceManager(diagnostics)
    }

    @Test
    fun `registerStream and releaseSessionResources closes stream deterministically`() {
        val transferId = "test-session-stream-1"
        var streamClosed = false

        val stream = object : AutoCloseable {
            override fun close() {
                streamClosed = true
            }
        }

        resourceManager.registerStream(transferId, stream)
        assertEquals(1, resourceManager.getOpenStreamCount())

        resourceManager.releaseSessionResources(transferId)
        assertTrue(streamClosed)
        assertEquals(0, resourceManager.getOpenStreamCount())
    }

    @Test
    fun `registerTempFile and releaseSessionResources deletes temp file`() {
        val transferId = "test-session-file-1"
        val tempFile = File.createTempFile("mesh_test_", ".tmp")
        assertTrue(tempFile.exists())

        resourceManager.registerTempFile(transferId, tempFile)
        resourceManager.releaseSessionResources(transferId)

        assertFalse(tempFile.exists())
    }

    @Test
    fun `registerJob and releaseSessionResources cancels job`() {
        val transferId = "test-session-job-1"
        val job = Job()
        assertTrue(job.isActive)

        resourceManager.registerJob(transferId, job)
        assertEquals(1, resourceManager.getActiveJobCount())

        resourceManager.releaseSessionResources(transferId)
        assertTrue(job.isCancelled)
        assertEquals(0, resourceManager.getActiveJobCount())
    }
}
