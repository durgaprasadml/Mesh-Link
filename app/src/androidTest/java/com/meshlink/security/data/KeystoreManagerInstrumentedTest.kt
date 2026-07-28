package com.meshlink.security.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@RunWith(AndroidJUnit4::class)
class KeystoreManagerInstrumentedTest {

    private lateinit var keystoreManager: KeystoreManagerImpl

    @Before
    fun setup() {
        // Clear any existing key for clean test state
        try {
            val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE).apply { load(null) }
            keyStore.deleteEntry(SecurityConstants.DB_MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore
        }
        
        keystoreManager = KeystoreManagerImpl()
    }

    @Test
    fun `encrypt and decrypt successfully`() {
        val plaintext = "test_seed_material".toByteArray(Charsets.UTF_8)
        
        val ciphertext = keystoreManager.encrypt(plaintext)
        assertNotNull(ciphertext)
        assertTrue(ciphertext.isNotEmpty())
        
        val decrypted = keystoreManager.decrypt(ciphertext)
        assertNotNull(decrypted)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `decrypt throws SecurityRecoveryException with corrupted ciphertext`() {
        val plaintext = "test_seed_material".toByteArray(Charsets.UTF_8)
        val ciphertext = keystoreManager.encrypt(plaintext)
        
        // Corrupt ciphertext
        if (ciphertext.isNotEmpty()) {
            ciphertext[ciphertext.size - 1] = (ciphertext[ciphertext.size - 1] + 1).toByte()
        }
        
        assertThrows(SecurityRecoveryException::class.java) {
            keystoreManager.decrypt(ciphertext)
        }
    }

    @Test
    fun `decrypt throws SecurityRecoveryException when key is deleted`() {
        val plaintext = "test_seed_material".toByteArray(Charsets.UTF_8)
        val ciphertext = keystoreManager.encrypt(plaintext)
        
        // Delete key to simulate invalidation/loss
        val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE).apply { load(null) }
        keyStore.deleteEntry(SecurityConstants.DB_MASTER_KEY_ALIAS)
        
        assertThrows(SecurityRecoveryException::class.java) {
            keystoreManager.decrypt(ciphertext)
        }
    }
}
