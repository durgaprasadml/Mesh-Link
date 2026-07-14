package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrityVerifier @Inject constructor() {

    companion object {
        private const val TAG = "IntegrityVerifier"
    }

    /**
     * Verifies the SHA-256 checksum of an assembled file.
     */
    fun verifyFileChecksum(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) {
            MeshLogger.w(TAG, "No expected checksum provided, skipping verification.")
            return true // Assume valid if no checksum provided (e.g. legacy transfers)
        }

        if (!file.exists()) return false

        try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            
            val computedHash = md.digest().joinToString("") { "%02x".format(it) }
            val isValid = computedHash == expectedSha256
            
            if (!isValid) {
                MeshLogger.e(TAG, "Checksum mismatch! Expected: $expectedSha256, Computed: $computedHash")
            }
            return isValid
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error calculating checksum: ${e.message}")
            return false
        }
    }
    
    fun calculateFileChecksum(file: File): String? {
        if (!file.exists()) return null
        
        try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to calculate checksum: ${e.message}")
            return null
        }
    }
}
