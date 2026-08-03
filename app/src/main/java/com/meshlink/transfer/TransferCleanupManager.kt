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
 * Asynchronous cleanup manager responsible for purging orphaned chunk files, cancelled/failed transfer caches,
 * expired temporary files, and incomplete staging folders.
 * Strictly executes on Dispatchers.IO and never deletes directories belonging to active transfer sessions.
 */
@Singleton
class TransferCleanupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: TransportDiagnostics
) {
    companion object {
        private const val TAG = "TransferCleanupManager"
        private const val CACHE_DIR_NAME = "transfers"
        const val DEFAULT_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val stagingDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME)
    }

    /**
     * Executes a full cleanup sweep safely without impacting active sessions.
     */
    suspend fun runFullCleanup(
        activeSessions: List<TransferSession>,
        maxAgeMs: Long = DEFAULT_EXPIRATION_MS
    ): CleanupResult = withContext(Dispatchers.IO) {
        val startTimeMs = System.currentTimeMillis()
        val activeIds = activeSessions.filter { it.isActive() }.map { it.transferId }.toSet()
        val cancelledFailedSessions = activeSessions.filter { it.requiresCleanup() }

        var cleanedOrphans = 0
        var cleanedCancelled = 0
        var cleanedExpired = 0
        var bytesReclaimed = 0L

        try {
            if (!stagingDir.exists()) {
                return@withContext CleanupResult(0, 0, 0, 0L, System.currentTimeMillis() - startTimeMs)
            }

            // 1. Clean cancelled and failed transfers
            for (session in cancelledFailedSessions) {
                val sessionDir = File(stagingDir, session.transferId)
                if (sessionDir.exists()) {
                    val size = calculateDirectorySize(sessionDir)
                    if (sessionDir.deleteRecursively()) {
                        cleanedCancelled++
                        bytesReclaimed += size
                    }
                }
            }

            // 2. Clean orphaned chunk files & incomplete directories not in activeIds
            stagingDir.listFiles()?.forEach { sessionDir ->
                if (sessionDir.isDirectory) {
                    val transferId = sessionDir.name
                    if (!activeIds.contains(transferId)) {
                        val isExpired = (System.currentTimeMillis() - sessionDir.lastModified()) > maxAgeMs
                        val size = calculateDirectorySize(sessionDir)
                        if (sessionDir.deleteRecursively()) {
                            if (isExpired) {
                                cleanedExpired++
                            } else {
                                cleanedOrphans++
                            }
                            bytesReclaimed += size
                        }
                    }
                }
            }

            val durationMs = System.currentTimeMillis() - startTimeMs
            diagnostics.logCleanupEvent(
                "FULL_CLEANUP_COMPLETE",
                "Orphans=$cleanedOrphans, Cancelled=$cleanedCancelled, Expired=$cleanedExpired, ReclaimedBytes=$bytesReclaimed",
                true
            )
            MeshLogger.i(TAG, "Cleanup completed in ${durationMs}ms. Reclaimed $bytesReclaimed bytes.")
            CleanupResult(cleanedOrphans, cleanedCancelled, cleanedExpired, bytesReclaimed, durationMs)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error during cleanup sweep: ${e.message}", e)
            diagnostics.logCleanupEvent("CLEANUP_ERROR", e.message ?: "Unknown", false)
            CleanupResult(cleanedOrphans, cleanedCancelled, cleanedExpired, bytesReclaimed, System.currentTimeMillis() - startTimeMs)
        }
    }

    /**
     * Cleans orphaned chunk files for non-active sessions.
     */
    suspend fun cleanOrphanedChunks(activeTransferIds: Set<String>): Int = withContext(Dispatchers.IO) {
        var count = 0
        if (!stagingDir.exists()) return@withContext 0
        stagingDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && !activeTransferIds.contains(dir.name)) {
                if (dir.deleteRecursively()) count++
            }
        }
        count
    }

    /**
     * Deletes staging directories for cancelled or failed sessions.
     */
    suspend fun cleanSessionCache(transferId: String): Boolean = withContext(Dispatchers.IO) {
        val sessionDir = File(stagingDir, transferId)
        if (sessionDir.exists()) {
            return@withContext sessionDir.deleteRecursively()
        }
        true
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

data class CleanupResult(
    val orphanedCleaned: Int,
    val cancelledFailedCleaned: Int,
    val expiredCleaned: Int,
    val bytesReclaimed: Long,
    val durationMs: Long
)
