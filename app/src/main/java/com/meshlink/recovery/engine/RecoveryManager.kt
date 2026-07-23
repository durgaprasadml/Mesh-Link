package com.meshlink.recovery.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class RecoveryMode {
    AUTOMATIC, MANUAL, SAFE_MODE
}

@Singleton
class RecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val integrityManager: IntegrityManager
) {

    fun attemptRecovery(targetDbFile: File, mode: RecoveryMode = RecoveryMode.AUTOMATIC): Boolean {
        val backupFile = backupManager.getLatestBackup()
        
        if (backupFile == null || !backupFile.exists()) {
            return false // Nothing to recover from
        }

        // Verify backup integrity before restoring
        val expectedHashFile = File(context.filesDir, "backups/mesh_db_backup.sha256")
        if (expectedHashFile.exists()) {
            val expectedHash = expectedHashFile.readText()
            val actualHash = integrityManager.computeSha256(backupFile)
            if (actualHash != expectedHash) {
                // The backup itself is corrupted, do not restore
                return false
            }
        }

        return try {
            // Perform restoration
            if (targetDbFile.exists()) {
                // Rename current corrupted DB just in case we need forensic analysis later
                val forensicDb = File(context.filesDir, "corrupted_db_${System.currentTimeMillis()}.db")
                targetDbFile.renameTo(forensicDb)
            }
            
            backupFile.copyTo(targetDbFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
