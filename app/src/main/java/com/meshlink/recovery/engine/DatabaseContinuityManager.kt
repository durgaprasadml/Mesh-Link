package com.meshlink.recovery.engine

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseContinuityManager @Inject constructor(
    private val integrityManager: IntegrityManager,
    private val recoveryManager: RecoveryManager,
    private val backupManager: BackupManager
) {

    fun ensureDatabaseContinuity(dbFile: File): Boolean {
        // Step 1: Check primary DB integrity
        if (integrityManager.verifyDatabaseIntegrity(dbFile)) {
            // DB is healthy, create a rolling backup for future failures
            backupManager.createBackup(dbFile)
            return true
        }

        // Step 2: Primary DB is corrupted or truncated. Attempt automated recovery.
        val recoverySuccess = recoveryManager.attemptRecovery(dbFile, RecoveryMode.AUTOMATIC)
        if (recoverySuccess) {
            // Recovery worked, the app can continue launching normally.
            return true
        }

        // Step 3: Backup doesn't exist or is also corrupted.
        // We MUST NOT silently delete and recreate the DB, as that loses user identities and chat history.
        // The UI should intercept this and boot into a "Safe Mode" read-only or diagnostic state.
        return false
    }
}
