package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production Integrity Verifier for Mesh Link Media Subsystem.
 * Provides streaming SHA-256 calculation for large files (100MB+)
 * and per-chunk CRC32 verification.
 */
@Singleton
class IntegrityVerifier @Inject constructor() {

    companion object {
        private const val TAG = "IntegrityVerifier"
        private const val STREAM_BUFFER_SIZE = 16 * 1024 // 16 KB buffer
    }

    /**
     * Calculates the CRC32 checksum of a raw byte array chunk.
     */
    fun calculateCrc32(data: ByteArray): Long {
        val crc = CRC32()
        crc.update(data)
        return crc.value
    }

    /**
     * Verifies that the raw chunk data matches the expected CRC32 checksum.
     */
    fun verifyCrc32(data: ByteArray, expectedCrc32: Long): Boolean {
        return calculateCrc32(data) == expectedCrc32
    }

    /**
     * Verifies the SHA-256 checksum of an assembled file using zero-copy streaming.
     */
    fun verifyFileChecksum(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) {
            MeshLogger.w(TAG, "No expected checksum provided, skipping verification.")
            return true
        }

        if (!file.exists()) return false

        return try {
            val computedHash = calculateFileChecksum(file)
            val isValid = computedHash.equals(expectedSha256, ignoreCase = true)

            if (!isValid) {
                MeshLogger.e(TAG, "Checksum mismatch for ${file.name}! Expected: $expectedSha256, Computed: $computedHash")
            }
            isValid
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error calculating checksum for ${file.name}: ${e.message}")
            false
        }
    }

    /**
     * Streaming SHA-256 calculation directly from an InputStream.
     */
    fun calculateStreamChecksum(inputStream: InputStream): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = BufferPool.borrowBuffer(STREAM_BUFFER_SIZE)
            try {
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (bytesRead > 0) {
                        md.update(buffer, 0, bytesRead)
                    }
                }
            } finally {
                BufferPool.returnBuffer(buffer)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to calculate stream SHA-256: ${e.message}")
            null
        }
    }

    /**
     * Streaming SHA-256 calculation for a File without loading it into RAM.
     */
    fun calculateFileChecksum(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            file.inputStream().buffered().use { stream ->
                calculateStreamChecksum(stream)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to calculate file SHA-256 for ${file.name}: ${e.message}")
            null
        }
    }
}
