package com.meshlink.transfer

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.routing.engine.TransportDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cache manager responsible for tracking total staging cache size, purging expired transfer artifacts,
 * and enforcing dynamic cache limits scaling across device storage limits.
 */
@Singleton
class TransferCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: TransportDiagnostics
) {
    companion object {
        private const val TAG = "TransferCacheManager"
        private const val CACHE_DIR_NAME = "transfers"
        const val DEFAULT_MAX_CACHE_BYTES = 500 * 1024 * 1024L // 500 MB upper bound
        const val DEFAULT_CACHE_TTL_MS = 12 * 60 * 60 * 1000L // 12 hours for completed/failed artifacts
    }

    private val stagingDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME)
    }

    /**
     * Calculates the dynamic maximum cache size based on available disk space and configurable upper bound.
     * Dynamic threshold: min(10% of total cache directory disk space, configurable upper bound).
     */
    fun getDynamicCacheLimit(maxUpperBoundBytes: Long = DEFAULT_MAX_CACHE_BYTES): Long {
        val totalSpace = context.cacheDir.totalSpace
        val tenPercentSpace = if (totalSpace > 0) totalSpace / 10 else maxUpperBoundBytes
        return minOf(tenPercentSpace, maxUpperBoundBytes).coerceAtLeast(50 * 1024 * 1024L) // min 50MB
    }

    /**
     * Calculates total bytes consumed by all transfer staging directories.
     */
    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        if (!stagingDir.exists()) return@withContext 0L
        calculateDirectorySize(stagingDir)
    }

    /**
     * Purges expired transfer artifacts older than [ttlMs] while skipping active sessions.
     */
    suspend fun purgeExpiredArtifacts(
        activeTransferIds: Set<String>,
        ttlMs: Long = DEFAULT_CACHE_TTL_MS
    ): Long = withContext(Dispatchers.IO) {
        var reclaimedBytes = 0L
        if (!stagingDir.exists()) return@withContext 0L

        val now = System.currentTimeMillis()
        stagingDir.listFiles()?.forEach { sessionDir ->
            if (sessionDir.isDirectory && !activeTransferIds.contains(sessionDir.name)) {
                val age = now - sessionDir.lastModified()
                if (age > ttlMs) {
                    val size = calculateDirectorySize(sessionDir)
                    if (sessionDir.deleteRecursively()) {
                        reclaimedBytes += size
                        MeshLogger.d(TAG, "Purged expired session cache ${sessionDir.name} ($size bytes)")
                    }
                }
            }
        }
        if (reclaimedBytes > 0) {
            diagnostics.logCleanupEvent("CACHE_PURGE_EXPIRED", "Reclaimed $reclaimedBytes bytes", true)
        }
        reclaimedBytes
    }

    /**
     * Trims staging cache down to [targetMaxBytes] using LRU order over inactive transfer folders.
     */
    suspend fun trimCache(
        activeTransferIds: Set<String>,
        targetMaxBytes: Long = getDynamicCacheLimit()
    ): Long = withContext(Dispatchers.IO) {
        var currentSize = getCacheSizeBytes()
        if (currentSize <= targetMaxBytes || !stagingDir.exists()) {
            return@withContext 0L
        }

        var reclaimedBytes = 0L
        val inactiveDirs = stagingDir.listFiles()
            ?.filter { it.isDirectory && !activeTransferIds.contains(it.name) }
            ?.sortedBy { it.lastModified() } ?: emptyList()

        for (dir in inactiveDirs) {
            if (currentSize <= targetMaxBytes) break
            val size = calculateDirectorySize(dir)
            if (dir.deleteRecursively()) {
                currentSize -= size
                reclaimedBytes += size
            }
        }

        diagnostics.logCleanupEvent("CACHE_TRIMMED", "Reclaimed $reclaimedBytes bytes, Current=$currentSize", true)
        reclaimedBytes
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
    }
}
