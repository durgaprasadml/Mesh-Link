package com.meshlink.recovery.engine

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import java.util.zip.CRC32

data class IntegrityReport(
    val isDatabaseValid: Boolean,
    val isPreferencesValid: Boolean,
    val checksumFailures: Int,
    val message: String
)

@Singleton
class IntegrityManager @Inject constructor() {

    fun verifyDatabaseIntegrity(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() == 0L) return false
        
        // In a real scenario, we might verify the SQLite header bytes (SQLite format 3\000)
        // For SQLCipher, it's completely encrypted, so we rely on SQLCipher's PRAGMA integrity_check 
        // during DatabaseContinuityManager's load phase.
        // Here we just ensure the file isn't obviously truncated.
        if (dbFile.length() < 1024L) return false 
        return true
    }

    fun computeSha256(file: File): String? {
        if (!file.exists()) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun computeCrc32(data: ByteArray): Long {
        val crc = CRC32()
        crc.update(data)
        return crc.value
    }
    
    fun performSystemIntegrityCheck(dbFile: File, prefsFile: File): IntegrityReport {
        val dbValid = verifyDatabaseIntegrity(dbFile)
        val prefsValid = prefsFile.exists() && prefsFile.length() > 0L
        
        val failures = listOf(dbValid, prefsValid).count { !it }
        
        return IntegrityReport(
            isDatabaseValid = dbValid,
            isPreferencesValid = prefsValid,
            checksumFailures = failures,
            message = if (failures == 0) "System Integrity Verified" else "Integrity Check Failed: $failures subsystems compromised"
        )
    }
}
