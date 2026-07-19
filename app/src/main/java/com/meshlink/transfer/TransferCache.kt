package com.meshlink.transfer

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TransferCache @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TransferCache"
        private const val CACHE_DIR_NAME = "mesh_transfer_staging"
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
            chunkFile.writeBytes(data)
            true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to write chunk $chunkIndex for $transferId: ${e.message}")
            false
        }
    }

    suspend fun readChunk(transferId: String, chunkIndex: Int): ByteArray? = withContext(Dispatchers.IO) {
        val chunkFile = File(stagingDir, "$transferId/$chunkIndex.chk")
        if (chunkFile.exists()) {
            try {
                chunkFile.readBytes()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to read chunk $chunkIndex for $transferId: ${e.message}")
                null
            }
        } else {
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

    suspend fun assembleFile(transferId: String, totalChunks: Int, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionDir = File(stagingDir, transferId)
            if (!sessionDir.exists()) return@withContext false

            outputFile.outputStream().use { out ->
                for (i in 0 until totalChunks) {
                    val chunkFile = File(sessionDir, "$i.chk")
                    if (!chunkFile.exists()) {
                        MeshLogger.e(TAG, "Missing chunk $i during assembly of $transferId")
                        return@withContext false
                    }
                    chunkFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
            }
            true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Assembly failed for $transferId: ${e.message}")
            false
        }
    }

    suspend fun cleanUpSession(transferId: String) = withContext(Dispatchers.IO) {
        val sessionDir = File(stagingDir, transferId)
        if (sessionDir.exists()) {
            sessionDir.deleteRecursively()
            MeshLogger.d(TAG, "Cleaned up cache for $transferId")
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
            stagingDir.mkdirs()
            MeshLogger.w(TAG, "Transfer cache fully cleared due to memory pressure")
        }
    }
}
