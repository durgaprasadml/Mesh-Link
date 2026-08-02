package com.meshlink.transfer

import com.meshlink.common.pool.BufferPool
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class ChunkManager @Inject constructor() {

    companion object {
        const val BLE_MTU_CHUNK_BYTES = 180 
        const val WIFI_MTU_CHUNK_BYTES_64K = 64 * 1024
        const val WIFI_MTU_CHUNK_BYTES_128K = 128 * 1024
    }

    var defaultWifiChunkSize: Int = WIFI_MTU_CHUNK_BYTES_64K

    fun calculateChunkSize(transportType: TransportType, customChunkSize: Int? = null): Int {
        if (customChunkSize != null && customChunkSize > 0) return customChunkSize
        return when (transportType) {
            TransportType.WIFI_DIRECT -> defaultWifiChunkSize
            TransportType.BLE -> BLE_MTU_CHUNK_BYTES
            else -> BLE_MTU_CHUNK_BYTES
        }
    }

    fun getTotalChunks(fileSize: Long, transportType: TransportType, customChunkSize: Int? = null): Int {
        val chunkSize = calculateChunkSize(transportType, customChunkSize)
        if (fileSize <= 0) return 1
        return ceil(fileSize.toDouble() / chunkSize).toInt()
    }

    /**
     * Reads a specific chunk from the file without loading the entire file into memory.
     */
    fun readChunkFromFile(file: File, chunkIndex: Int, chunkSize: Int): ByteArray? {
        if (!file.exists() || !file.canRead()) return null
        
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val offset = chunkIndex.toLong() * chunkSize
                if (offset >= raf.length()) return null
                
                raf.seek(offset)
                
                val bytesToRead = minOf(chunkSize.toLong(), raf.length() - offset).toInt()
                val buffer = BufferPool.borrowBuffer(bytesToRead)
                raf.readFully(buffer)
                buffer
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes SHA-256 digest of a specific chunk.
     */
    fun calculateChunkChecksum(chunkBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(chunkBytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

