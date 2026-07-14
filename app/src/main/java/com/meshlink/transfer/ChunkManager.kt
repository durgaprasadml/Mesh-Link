package com.meshlink.transfer

import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class ChunkManager @Inject constructor() {

    companion object {
        // Base64 expansion is roughly 4/3. 
        // 300 bytes of raw data -> 400 bytes Base64 -> fits in 512 MTU
        const val BLE_MTU_CHUNK_BYTES = 300 
        
        // Wi-Fi can handle much larger packets. Let's do 64KB chunks.
        // 64KB -> ~85KB Base64
        const val WIFI_MTU_CHUNK_BYTES = 64 * 1024 
    }

    fun calculateChunkSize(transportType: TransportType): Int {
        return when (transportType) {
            TransportType.WIFI_DIRECT -> WIFI_MTU_CHUNK_BYTES
            TransportType.BLE -> BLE_MTU_CHUNK_BYTES
            else -> BLE_MTU_CHUNK_BYTES
        }
    }

    fun getTotalChunks(fileSize: Long, transportType: TransportType): Int {
        val chunkSize = calculateChunkSize(transportType)
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
                val buffer = ByteArray(bytesToRead)
                raf.readFully(buffer)
                buffer
            }
        } catch (e: Exception) {
            null
        }
    }
}
