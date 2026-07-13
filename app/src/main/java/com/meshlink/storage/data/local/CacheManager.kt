package com.meshlink.storage.data.local

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CacheManager"
        private const val MEDIA_CACHE_DIR = "media_cache"
        private const val THUMB_CACHE_DIR = "thumb_cache"
        private const val TRANSFER_CHUNKS_DIR = "transfer_chunks"

        // Default constraints (can be configured via settings later)
        private const val MEDIA_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
        private const val MAX_THUMB_CACHE_SIZE = 50L * 1024 * 1024 // 50 MB
    }

    private fun getMediaCacheDir(): File {
        val dir = File(context.cacheDir, MEDIA_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getThumbCacheDir(): File {
        val dir = File(context.cacheDir, THUMB_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTransferChunksDir(): File {
        val dir = File(context.cacheDir, TRANSFER_CHUNKS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun clearTemporaryChunks() {
        withContext(Dispatchers.IO) {
            try {
                val dir = getTransferChunksDir()
                if (dir.exists()) {
                    dir.listFiles()?.forEach { it.delete() }
                    MeshLogger.d(TAG, "Cleared temporary transfer chunks.")
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to clear transfer chunks", e)
            }
            Unit
        }
    }

    suspend fun enforceQuotas() {
        withContext(Dispatchers.IO) {
            try {
                MeshLogger.d(TAG, "Enforcing cache quotas...")
                
                // 1. Delete media older than TTL
                val mediaDir = getMediaCacheDir()
                if (mediaDir.exists()) {
                    val now = System.currentTimeMillis()
                    mediaDir.listFiles()?.forEach { file ->
                        if (now - file.lastModified() > MEDIA_TTL_MS) {
                            file.delete()
                        }
                    }
                }

                // 2. LRU Evict thumbnails if exceeding max size
                val thumbDir = getThumbCacheDir()
                if (thumbDir.exists()) {
                    val files = thumbDir.listFiles()?.toList() ?: emptyList()
                    var totalSize = files.sumOf { it.length() }
                    
                    if (totalSize > MAX_THUMB_CACHE_SIZE) {
                        val sortedFiles = files.sortedBy { it.lastModified() }
                        for (file in sortedFiles) {
                            if (totalSize <= MAX_THUMB_CACHE_SIZE) break
                            totalSize -= file.length()
                            file.delete()
                        }
                    }
                }
                MeshLogger.d(TAG, "Cache quotas enforced successfully.")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to enforce cache quotas", e)
            }
            Unit
        }
    }
}
