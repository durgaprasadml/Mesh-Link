package com.meshlink.security.data

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManagerImpl @Inject constructor() : KeystoreManager {

    private fun getKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE).apply { load(null) }
        
        if (!keyStore.containsAlias(SecurityConstants.DB_MASTER_KEY_ALIAS)) {
            generateKeystoreKeyWithStrongBoxFallback()
        }
        return keyStore.getKey(SecurityConstants.DB_MASTER_KEY_ALIAS, null) as SecretKey
    }

    @android.annotation.SuppressLint("NewApi")
    private fun generateKeystoreKeyWithStrongBoxFallback() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, SecurityConstants.ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(SecurityConstants.DB_MASTER_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(SecurityConstants.AES_KEY_SIZE_BITS)
            
        // Attempt StrongBox first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                keyGenerator.init(specBuilder.setIsStrongBoxBacked(true).build())
                keyGenerator.generateKey()
                return
            } catch (e: StrongBoxUnavailableException) {
                // Ignore, fallback below
            } catch (e: ProviderException) {
                // Ignore, fallback below
            } catch (e: InvalidAlgorithmParameterException) {
                // Ignore, fallback below
            }
        }

        // Fallback to normal Keystore
        keyGenerator.init(specBuilder.setIsStrongBoxBacked(false).build())
        keyGenerator.generateKey()
    }

    @Throws(SecurityRecoveryException::class)
    override fun encrypt(plaintext: ByteArray): ByteArray {
        var attempt = 0
        while (attempt < 3) {
            try {
                val key = getKeystoreKey()
                val cipher = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                
                val iv = cipher.iv
                val ciphertext = cipher.doFinal(plaintext)
                
                val result = ByteArray(iv.size + ciphertext.size)
                System.arraycopy(iv, 0, result, 0, iv.size)
                System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)
                return result
            } catch (e: Exception) {
                attempt++
                if (attempt >= 3) {
                    com.meshlink.common.logger.MeshLogger.e("KeystoreManager", "Keystore DB encrypt failed after 3 retries", e)
                    throw SecurityRecoveryException("Keystore DB encrypt failed after 3 retries", e)
                }
            }
        }
        throw SecurityRecoveryException("Keystore DB encrypt failed")
    }

    @Throws(SecurityRecoveryException::class)
    override fun decrypt(ciphertext: ByteArray): ByteArray {
        var attempt = 0
        while (attempt < 3) {
            try {
                val key = getKeystoreKey()
                val cipher = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
                
                val iv = ciphertext.copyOfRange(0, SecurityConstants.GCM_IV_LENGTH_BYTES)
                val encrypted = ciphertext.copyOfRange(SecurityConstants.GCM_IV_LENGTH_BYTES, ciphertext.size)
                
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv))
                
                return cipher.doFinal(encrypted)
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                com.meshlink.common.logger.MeshLogger.e("KeystoreManager", "Keystore DB key permanently invalidated")
                // Regenerate the master key but do NOT return empty dummy array
                try {
                    val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE).apply { load(null) }
                    keyStore.deleteEntry(SecurityConstants.DB_MASTER_KEY_ALIAS)
                    generateKeystoreKeyWithStrongBoxFallback()
                } catch (ignore: Exception) {}
                
                // Bubble up failure to avoid destructive Room fallback
                com.meshlink.common.logger.MeshLogger.e("KeystoreManager", "Keystore DB key permanently invalidated", e)
                throw SecurityRecoveryException("Keystore DB key permanently invalidated", e)
            } catch (e: Exception) {
                attempt++
                if (attempt >= 3) {
                    val msg = e.javaClass.simpleName
                    com.meshlink.common.logger.MeshLogger.e("KeystoreManager", "Keystore DB decrypt failed after 3 retries: $msg", e)
                    throw SecurityRecoveryException("Keystore DB decrypt failed after 3 retries: $msg", e)
                }
            }
        }
        throw SecurityRecoveryException("Keystore DB decrypt failed")
    }
}
