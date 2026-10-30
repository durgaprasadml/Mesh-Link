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

    suspend fun clearMediaCache(): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        val targetDirs = listOf(
            getMediaCacheDir(),
            getThumbCacheDir(),
            getTransferChunksDir(),
            File(context.cacheDir, "transfers")
        )
        for (dir in targetDirs) {
            if (dir.exists()) {
                dir.listFiles()?.forEach { child ->
                    val size = calculateSize(child)
                    if (child.deleteRecursively()) {
                        freedBytes += size
                    }
                }
            }
        }
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("wifi_rx_") && file.name.endsWith(".tmp")) {
                val len = file.length()
                if (file.delete()) {
                    freedBytes += len
                }
            }
        }
        MeshLogger.i(TAG, "Cleared media cache, freed $freedBytes bytes")
        freedBytes
    }

    suspend fun clearOrphanFiles(referencedPaths: Set<String>): Long = withContext(Dispatchers.IO) {
        val normalizedRefs = referencedPaths.mapNotNull { path ->
            try { File(path).canonicalPath } catch (_: Exception) { File(path).absolutePath }
        }.toSet()

        var freedBytes = 0L
        val managedDirs = listOf(
            File(context.filesDir, "media"),
            File(context.filesDir, "voice"),
            File(context.filesDir, "sos"),
            getMediaCacheDir()
        )
        for (dir in managedDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val canonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                if (canonical !in normalizedRefs && file.absolutePath !in referencedPaths) {
                    val len = file.length()
                    if (file.delete()) {
                        freedBytes += len
                    }
                }
            }
        }
        MeshLogger.i(TAG, "Cleared orphan media files, freed $freedBytes bytes")
        freedBytes
    }

    suspend fun getStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        val dbName = com.meshlink.security.data.SecurityConstants.DB_NAME
        val dbMain = context.getDatabasePath(dbName)
        val dbWal = File(dbMain.path + "-wal")
        val dbShm = File(dbMain.path + "-shm")
        val databaseBytes = (if (dbMain.exists()) dbMain.length() else 0L) +
            (if (dbWal.exists()) dbWal.length() else 0L) +
            (if (dbShm.exists()) dbShm.length() else 0L)

        val mediaCacheBytes = calculateSize(getMediaCacheDir()) +
            calculateSize(getThumbCacheDir()) +
            calculateSize(getTransferChunksDir()) +
            calculateSize(File(context.cacheDir, "transfers")) +
            calculateSize(File(context.filesDir, "media"))

        val voiceNotesBytes = calculateSize(File(context.filesDir, "voice")) +
            (context.filesDir.listFiles()?.filter {
                it.isFile && (it.name.endsWith(".m4a") || it.name.endsWith(".aac") || it.name.endsWith(".wav") || it.name.endsWith(".opus"))
            }?.sumOf { it.length() } ?: 0L)

        val freeSpaceBytes = context.filesDir.usableSpace.coerceAtLeast(0L)
        val totalSpaceBytes = context.filesDir.totalSpace.coerceAtLeast(
            databaseBytes + mediaCacheBytes + voiceNotesBytes + freeSpaceBytes
        ).coerceAtLeast(1L)

        StorageBreakdown(
            databaseBytes = databaseBytes,
            mediaCacheBytes = mediaCacheBytes,
            voiceNotesBytes = voiceNotesBytes,
            freeSpaceBytes = freeSpaceBytes,
            totalSpaceBytes = totalSpaceBytes
        )
    }

    private fun calculateSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { calculateSize(it) } ?: 0L
    }
}

data class StorageBreakdown(
    val databaseBytes: Long = 0L,
    val mediaCacheBytes: Long = 0L,
    val voiceNotesBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L,
    val totalSpaceBytes: Long = 1L
)
