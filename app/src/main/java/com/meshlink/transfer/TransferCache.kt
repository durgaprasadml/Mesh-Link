package com.meshlink.transfer

import android.content.Context
import com.google.gson.Gson
import com.meshlink.common.logger.MeshLogger
import com.meshlink.transfer.TransferSession
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

    private val gson = Gson()

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

    suspend fun persistSession(session: TransferSession) = withContext(Dispatchers.IO) {
        try {
            val sessionDir = File(stagingDir, session.transferId)
            if (!sessionDir.exists()) sessionDir.mkdirs()
            val metaFile = File(sessionDir, "session.json")
            metaFile.writeText(gson.toJson(session))
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to persist session ${session.transferId}: ${e.message}")
        }
    }

    suspend fun loadPersistedSessions(): List<TransferSession> = withContext(Dispatchers.IO) {
        val sessions = mutableListOf<TransferSession>()
        if (stagingDir.exists()) {
            stagingDir.listFiles()?.forEach { sessionDir ->
                if (sessionDir.isDirectory) {
                    val metaFile = File(sessionDir, "session.json")
                    if (metaFile.exists()) {
                        try {
                            val json = metaFile.readText()
                            val session = gson.fromJson(json, TransferSession::class.java)
                            if (session != null) sessions.add(session)
                        } catch (e: Exception) {
                            MeshLogger.e(TAG, "Failed to load session from ${sessionDir.name}: ${e.message}")
                        }
                    }
                }
            }
        }
        sessions
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
