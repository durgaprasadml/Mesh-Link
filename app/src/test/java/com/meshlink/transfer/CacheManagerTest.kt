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
class CacheManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val diagnostics = mockk<TransportDiagnostics>(relaxed = true)

    private lateinit var cacheDir: File
    private lateinit var transfersDir: File
    private lateinit var cacheManager: TransferCacheManager

    @Before
    fun setUp() {
        cacheDir = tempFolder.newFolder("cache")
        transfersDir = File(cacheDir, "transfers").also { it.mkdirs() }
        every { context.cacheDir } returns cacheDir

        cacheManager = TransferCacheManager(context, diagnostics)
    }

    @Test
    fun `getDynamicCacheLimit returns valid lower and upper bounds`() {
        val limit = cacheManager.getDynamicCacheLimit(maxUpperBoundBytes = 100 * 1024 * 1024L)
        assertTrue(limit >= 50 * 1024 * 1024L)
        assertTrue(limit <= 100 * 1024 * 1024L)
    }

    @Test
    fun `purgeExpiredArtifacts purges expired inactive folders`() = runTest {
        val oldDir = File(transfersDir, "old-session").also { it.mkdirs() }
        File(oldDir, "0.chk").writeText("old_data")
        oldDir.setLastModified(System.currentTimeMillis() - 100_000L)

        val activeDir = File(transfersDir, "active-session").also { it.mkdirs() }
        File(activeDir, "0.chk").writeText("active_data")

        val activeIds = setOf("active-session")
        val reclaimed = cacheManager.purgeExpiredArtifacts(activeIds, ttlMs = 10_000L)

        assertTrue(reclaimed > 0L)
        assertFalse(oldDir.exists())
        assertTrue(activeDir.exists())
    }

    @Test
    fun `trimCache trims inactive folders when target limit exceeded`() = runTest {
        val dir1 = File(transfersDir, "session-1").also { it.mkdirs() }
        File(dir1, "data.bin").writeBytes(ByteArray(10_000))
        dir1.setLastModified(System.currentTimeMillis() - 50_000L)

        val dir2 = File(transfersDir, "session-2").also { it.mkdirs() }
        File(dir2, "data.bin").writeBytes(ByteArray(10_000))
        dir2.setLastModified(System.currentTimeMillis() - 10_000L)

        val reclaimed = cacheManager.trimCache(activeTransferIds = emptySet(), targetMaxBytes = 5_000L)

        assertTrue(reclaimed >= 10_000L)
        assertFalse(dir1.exists()) // Older directory removed first via LRU ordering
    }
}
