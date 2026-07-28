package com.meshlink.security.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DatabaseSecurityManagerTest {

    private lateinit var context: Context
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var databaseSecurityManager: DatabaseSecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keystoreManager = mockk(relaxed = true)
        
        // Mock default successful encryption/decryption
        every { keystoreManager.encrypt(any()) } answers { firstArg<ByteArray>() }
        every { keystoreManager.decrypt(any()) } answers { firstArg<ByteArray>() }

        databaseSecurityManager = DatabaseSecurityManager(context, keystoreManager)
    }

    @Test
    fun `getDatabasePassphrase returns secure passphrase bytes`() {
        val passphraseBytes = databaseSecurityManager.getDatabasePassphrase().getBytes()
        assertNotNull(passphraseBytes)
        assertTrue(passphraseBytes.isNotEmpty())
        
        // Calling it again should yield the same derived bytes (idempotency)
        val passphraseBytes2 = databaseSecurityManager.getDatabasePassphrase().getBytes()
        assertTrue(passphraseBytes.contentEquals(passphraseBytes2))
    }

    @Test
    fun `migration from legacy UUID passphrase executes without crash`() {
        val legacyPrefs = context.getSharedPreferences(SecurityConstants.DB_PREFS_NAME_LEGACY, Context.MODE_PRIVATE)
        legacyPrefs.edit().putString(SecurityConstants.KEY_LEGACY_PASSPHRASE, UUID.randomUUID().toString()).commit()

        val passphraseBytes = databaseSecurityManager.getDatabasePassphrase().getBytes()
        assertNotNull(passphraseBytes)
        assertTrue(passphraseBytes.isNotEmpty())
        
        val stillHasLegacy = legacyPrefs.contains(SecurityConstants.KEY_LEGACY_PASSPHRASE)
        assertTrue(!stillHasLegacy)
    }

    @Test
    fun `getDatabasePassphrase throws SecurityRecoveryException on keystore decrypt failure`() {
        // Run getDatabasePassphrase once to generate seed
        databaseSecurityManager.getDatabasePassphrase().getBytes()
        
        // Force decrypt failure
        every { keystoreManager.decrypt(any()) } throws SecurityRecoveryException("Keystore DB decrypt failed after 3 retries")
        
        assertThrows(SecurityRecoveryException::class.java) {
            databaseSecurityManager.getDatabasePassphrase().getBytes()
        }
    }

    @Test
    fun `getDatabasePassphrase throws SecurityRecoveryException on permanently invalidated key`() {
        databaseSecurityManager.getDatabasePassphrase().getBytes()
        
        // Force permanently invalidated key behavior
        every { keystoreManager.decrypt(any()) } throws SecurityRecoveryException("Keystore DB key permanently invalidated")
        
        assertThrows(SecurityRecoveryException::class.java) {
            databaseSecurityManager.getDatabasePassphrase().getBytes()
        }
    }

    @Test
    fun `getDatabasePassphrase throws SecurityRecoveryException if decrypted seed is empty`() {
        databaseSecurityManager.getDatabasePassphrase().getBytes()
        
        // Return empty array instead of decrypting properly
        every { keystoreManager.decrypt(any()) } returns ByteArray(0)
        
        val exception = assertThrows(SecurityRecoveryException::class.java) {
            databaseSecurityManager.getDatabasePassphrase().getBytes()
        }
        assertTrue(exception.message?.contains("Database seed cannot be recovered") == true)
    }
}
