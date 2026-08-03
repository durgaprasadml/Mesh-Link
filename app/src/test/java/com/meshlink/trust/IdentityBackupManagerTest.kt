package com.meshlink.trust

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class IdentityBackupManagerTest {

    private val identityManager: MeshIdentityManager = mockk(relaxed = true)
    private val auditLog: IdentityAuditLog = mockk(relaxed = true)
    private lateinit var backupManager: IdentityBackupManager

    @Before
    fun setup() {
        val testIdentity = MeshIdentity(
            meshId = "mesh-test-backup",
            publicKey = "pubkey_backup_123",
            displayName = "BackupUser"
        )
        every { identityManager.getOrCreateIdentity() } returns testIdentity

        backupManager = IdentityBackupManager(identityManager, auditLog)
    }

    @Test
    fun testExportAndRestoreBackup() {
        val passphrase = "SecurePassword123!"
        val encryptedBackup = backupManager.exportBackup(passphrase)

        assertNotNull(encryptedBackup)

        val restored = backupManager.restoreBackup(encryptedBackup, passphrase)
        assertEquals("mesh-test-backup", restored.meshId)
        assertEquals("pubkey_backup_123", restored.publicKey)
        assertEquals("BackupUser", restored.displayName)
    }
}
