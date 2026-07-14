package com.meshlink.storage.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.RandomAccessFile

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CacheManagerTest {

    private lateinit var context: Context
    private lateinit var cacheManager: CacheManager
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cacheDir = context.cacheDir
        cacheManager = CacheManager(context)
    }

    @After
    fun teardown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun `clearTemporaryChunks deletes all files in chunks directory`() = runTest {
        val chunkDir = File(cacheDir, "transfer_chunks").apply { mkdirs() }
        val chunk1 = File(chunkDir, "chunk_1").apply { createNewFile() }
        val chunk2 = File(chunkDir, "chunk_2").apply { createNewFile() }
        
        assertTrue(chunk1.exists())
        assertTrue(chunk2.exists())
        
        cacheManager.clearTemporaryChunks()
        
        assertFalse(chunk1.exists())
        assertFalse(chunk2.exists())
        assertTrue(chunkDir.exists()) // directory should remain
    }

    @Test
    fun `enforceQuotas deletes media older than 7 days`() = runTest {
        val mediaDir = File(cacheDir, "media_cache").apply { mkdirs() }
        val oldFile = File(mediaDir, "old.mp4").apply { createNewFile() }
        val newFile = File(mediaDir, "new.mp4").apply { createNewFile() }
        
        val now = System.currentTimeMillis()
        val eightDaysAgo = now - (8L * 24 * 60 * 60 * 1000)
        oldFile.setLastModified(eightDaysAgo)
        
        assertTrue(oldFile.exists())
        assertTrue(newFile.exists())
        
        cacheManager.enforceQuotas()
        
        assertFalse("Old file should be deleted", oldFile.exists())
        assertTrue("New file should be kept", newFile.exists())
    }

    @Test
    fun `enforceQuotas evicts LRU thumbnails if size exceeds max`() = runTest {
        val thumbDir = File(cacheDir, "thumb_cache").apply { mkdirs() }
        
        // Max size is 50MB. We'll create 3 files of 20MB each = 60MB total.
        // The oldest one should be deleted, leaving 40MB.
        val oldThumb = File(thumbDir, "old_thumb.jpg")
        val midThumb = File(thumbDir, "mid_thumb.jpg")
        val newThumb = File(thumbDir, "new_thumb.jpg")
        
        createFileOfSize(oldThumb, 20 * 1024 * 1024)
        createFileOfSize(midThumb, 20 * 1024 * 1024)
        createFileOfSize(newThumb, 20 * 1024 * 1024)
        
        val now = System.currentTimeMillis()
        oldThumb.setLastModified(now - 3000)
        midThumb.setLastModified(now - 2000)
        newThumb.setLastModified(now - 1000)
        
        val initialFiles = thumbDir.listFiles() ?: emptyArray()
        assertEquals(3, initialFiles.size)
        
        cacheManager.enforceQuotas()
        
        val finalFiles = thumbDir.listFiles() ?: emptyArray()
        assertEquals(2, finalFiles.size)
        assertFalse("Oldest thumb should be evicted", oldThumb.exists())
        assertTrue(midThumb.exists())
        assertTrue(newThumb.exists())
    }
    
    private fun createFileOfSize(file: File, sizeBytes: Long) {
        val raf = RandomAccessFile(file, "rw")
        raf.setLength(sizeBytes)
        raf.close()
    }
}
