package com.meshlink.recovery.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val integrityManager: IntegrityManager
) {

    // Simulating a rolling backup strategy.
    // In a real enterprise app, this would zip the Room .db file and encrypt it 
    // with a secondary recovery key derived from the Keystore.

    fun createBackup(dbFile: File): Boolean {
        if (!integrityManager.verifyDatabaseIntegrity(dbFile)) {
            // Never backup a corrupted database
            return false
        }
        
        return try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            
            // Rolling strategy: Only keep the most recent backup to save storage.
            val backupFile = File(backupDir, "mesh_db_backup.enc")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            // Simulating backup creation
            dbFile.copyTo(backupFile, overwrite = true)
            
            // Compute hash for recovery validation
            val hash = integrityManager.computeSha256(backupFile)
            File(backupDir, "mesh_db_backup.sha256").writeText(hash ?: "")
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getLatestBackup(): File? {
        val backupFile = File(context.filesDir, "backups/mesh_db_backup.enc")
        return if (backupFile.exists()) backupFile else null
    }
}
