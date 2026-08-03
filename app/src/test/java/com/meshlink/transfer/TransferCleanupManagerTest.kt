package com.meshlink.transfer

import android.content.Context
import com.meshlink.routing.engine.TransportDiagnostics
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCleanupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val diagnostics = mockk<TransportDiagnostics>(relaxed = true)

    private lateinit var cacheDir: File
    private lateinit var transfersDir: File
    private lateinit var cleanupManager: TransferCleanupManager

    @Before
    fun setUp() {
        cacheDir = tempFolder.newFolder("cache")
        transfersDir = File(cacheDir, "transfers").also { it.mkdirs() }
        every { context.cacheDir } returns cacheDir

        cleanupManager = TransferCleanupManager(context, diagnostics)
    }

    @Test
    fun `cleanSessionCache deletes specific session directory`() = runTest {
        val sessionDir = File(transfersDir, "session-123").also { it.mkdirs() }
        File(sessionDir, "0.chk").writeText("chunk_data")
        assertTrue(sessionDir.exists())

        val result = cleanupManager.cleanSessionCache("session-123")
        assertTrue(result)
        assertFalse(sessionDir.exists())
    }

    @Test
    fun `runFullCleanup cleans cancelled and failed sessions while preserving active sessions`() = runTest {
        // Active session directory
        val activeDir = File(transfersDir, "active-1").also { it.mkdirs() }
        File(activeDir, "0.chk").writeText("active_data")

        // Cancelled session directory
        val cancelledDir = File(transfersDir, "cancelled-1").also { it.mkdirs() }
        File(cancelledDir, "0.chk").writeText("cancelled_data")

        // Orphaned directory
        val orphanDir = File(transfersDir, "orphan-1").also { it.mkdirs() }
        File(orphanDir, "0.chk").writeText("orphan_data")

        val activeSessions = listOf(
            TransferSession("active-1", "s1", "t1", "a.png", "image/png", 100, 1, TransferDirection.OUTGOING, TransferState.SENDING),
            TransferSession("cancelled-1", "s1", "t1", "c.png", "image/png", 100, 1, TransferDirection.OUTGOING, TransferState.CANCELLED)
        )

        val result = cleanupManager.runFullCleanup(activeSessions, maxAgeMs = 0L) // 0 maxAge to consider orphan expired

        assertTrue(activeDir.exists())
        assertFalse(cancelledDir.exists())
        assertFalse(orphanDir.exists())
        assertTrue(result.bytesReclaimed > 0L)
    }
}
