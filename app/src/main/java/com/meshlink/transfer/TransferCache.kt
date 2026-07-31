package com.meshlink.transfer

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production File Cache Storage for Staging Transfer Chunks.
 * Note: Room database stores session metadata; binary chunk files stay in file staging cache.
 * Uses zero-copy buffered streaming for chunk assembly supporting 500MB+ transfers.
 */
@Singleton
class TransferCache @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TransferCache"
        private const val CACHE_DIR_NAME = "mesh_transfer_staging"
        private const val BUFFER_SIZE = 16 * 1024 // 16 KB buffer
    }

    private val stagingDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).also {
            if (!it.exists()) it.mkdirs()
        }
    }

    suspend fun initSessionCache(transferId: String): Boolean = withContext(Dispatchers.IO) {
        val sessionDir = File(stagingDir, transferId)
        if (!sessionDir.exists()) {
            return@withContext sessionDir.mkdirs()
        }
        true
    }

    suspend fun writeChunk(transferId: String, chunkIndex: Int, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionDir = File(stagingDir, transferId)
            if (!sessionDir.exists()) sessionDir.mkdirs()

            val chunkFile = File(sessionDir, "$chunkIndex.chk")
            FileOutputStream(chunkFile).use { fos ->
                fos.write(data)
            }
            true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to write chunk $chunkIndex for $transferId: ${e.message}")
            false
        }
    }

    suspend fun readChunk(transferId: String, chunkIndex: Int): ByteArray? = withContext(Dispatchers.IO) {
        val chunkFile = File(stagingDir, "$transferId/$chunkIndex.chk")
        if (!chunkFile.exists()) return@withContext null
        try {
            FileInputStream(chunkFile).use { fis ->
                fis.readBytes()
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to read chunk $chunkIndex for $transferId: ${e.message}")
            null
        }
    }

    suspend fun getReceivedChunkIndices(transferId: String): Set<Int> = withContext(Dispatchers.IO) {
        val sessionDir = File(stagingDir, transferId)
        if (!sessionDir.exists()) return@withContext emptySet()

        sessionDir.listFiles()
            ?.filter { it.name.endsWith(".chk") }
            ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    /**
     * Pure zero-copy streaming assembly of binary chunks into output file.
     * Prevents loading 100MB+ or 500MB+ files into heap RAM.
     */
    suspend fun assembleFile(transferId: String, totalChunks: Int, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionDir = File(stagingDir, transferId)
            if (!sessionDir.exists()) return@withContext false

            val buffer = BufferPool.borrowBuffer(BUFFER_SIZE)
            try {
                BufferedOutputStream(FileOutputStream(outputFile)).use { out ->
                    for (i in 0 until totalChunks) {
                        val chunkFile = File(sessionDir, "$i.chk")
                        if (!chunkFile.exists()) {
                            MeshLogger.e(TAG, "Missing chunk $i during streaming assembly of $transferId")
                            return@withContext false
                        }
                        BufferedInputStream(FileInputStream(chunkFile)).use { input ->
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                out.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    out.flush()
                }
            } finally {
                BufferPool.returnBuffer(buffer)
            }
            true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Streaming assembly failed for $transferId: ${e.message}", e)
            false
        }
    }

    suspend fun cleanUpSession(transferId: String) = withContext(Dispatchers.IO) {
        val sessionDir = File(stagingDir, transferId)
        if (sessionDir.exists()) {
            sessionDir.deleteRecursively()
            MeshLogger.d(TAG, "Cleaned up staging cache for $transferId")
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
            stagingDir.mkdirs()
            MeshLogger.w(TAG, "Transfer staging cache cleared")
        }
    }
}
