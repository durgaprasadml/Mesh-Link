package com.meshlink.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

/**
 * End-to-End encryption engine for the Mesh Link network.
 */
@Singleton
class MeshCryptoManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MeshCrypto"
    }

    // In-memory cache of derived AES keys per peer
    private val derivedKeys = java.util.concurrent.ConcurrentHashMap<String, SecretKey>()

    // EncryptedSharedPreferences for persistent peer public key storage
    private val peerKeyStore: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            val msg = e.javaClass.simpleName
            Log.e(TAG, "EncryptedSharedPreferences corruption detected, wiping and retrying: $msg")
            try {
                context.deleteSharedPreferences(SecurityConstants.PEER_KEYS_PREF)
                val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (wipeEx: Exception) {
                Log.e(TAG, "Failed to wipe corrupted Keystore/Prefs: ${wipeEx.javaClass.simpleName}")
            }
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SecurityConstants.PEER_KEYS_PREF,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ────────── Key Generation ──────────

    fun getOrCreatePublicKey(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return getOrCreateKeystorePublicKey()
            } catch (e: Exception) {
                Log.w(TAG, "Keystore ECDH failed, clearing and falling back: ${e.javaClass.simpleName}")
                try {
                    val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                    keyStore.load(null)
                    keyStore.deleteEntry(SecurityConstants.MESH_KEYSTORE_ALIAS)
                } catch (ignore: Exception) {}
            }
        }
        return getOrCreateSoftwarePublicKey()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getOrCreateKeystorePublicKey(): String {
        val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(SecurityConstants.MESH_KEYSTORE_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                SecurityConstants.ANDROID_KEYSTORE
            )
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    SecurityConstants.MESH_KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_AGREE_KEY
                )
                    .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                    .build()
            )
            keyPairGenerator.generateKeyPair()
        }

        val publicKey = keyStore.getCertificate(SecurityConstants.MESH_KEYSTORE_ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    private fun getOrCreateSoftwarePublicKey(): String {
        peerKeyStore.getString(SecurityConstants.SELF_PUBLIC_KEY_KEY, null)?.let { return it }

        return try {
            val kpg = KeyPairGenerator.getInstance(SecurityConstants.EC_ALGORITHM)
            kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()

            val pubBase64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
            val encoded = kp.private.encoded
            val privBase64 = Base64.encodeToString(encoded, Base64.NO_WRAP)

            peerKeyStore.edit()
                .putString(SecurityConstants.SELF_PUBLIC_KEY_KEY, pubBase64)
                .putString(SecurityConstants.SELF_PRIVATE_KEY_KEY, privBase64)
                .apply()

            java.util.Arrays.fill(encoded, 0.toByte())
            pubBase64
        } catch (e: Exception) {
            Log.e(TAG, "Software key generation failed: ${e.javaClass.simpleName}")
            ""
        }
    }

    private fun getPrivateKey(): java.security.PrivateKey {
        val privBase64 = try {
            peerKeyStore.getString(SecurityConstants.SELF_PRIVATE_KEY_KEY, null)
        } catch (e: Exception) {
            null
        }

        if (privBase64 != null) {
            val privBytes = Base64.decode(privBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            java.util.Arrays.fill(privBytes, 0.toByte())
            return privateKey
        }

        return try {
            val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(SecurityConstants.MESH_KEYSTORE_ALIAS, null) as java.security.PrivateKey
        } catch (e: Exception) {
            try {
                val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(SecurityConstants.MESH_KEYSTORE_ALIAS)
            } catch (ignore: Exception) {}
            throw e
        }
    }

    // ────────── Peer Key Management ──────────

    fun storePeerPublicKey(peerId: String, publicKeyBase64: String) {
        peerKeyStore.edit().putString(peerId, publicKeyBase64).apply()
        derivedKeys.remove(peerId)
    }

    fun getPeerPublicKey(peerId: String): String? {
        return peerKeyStore.getString(peerId, null)
    }

    fun hasPeerKey(peerId: String): Boolean {
        return peerKeyStore.contains(peerId)
    }

    // ────────── ECDH Key Agreement ──────────

    private fun deriveSharedKey(peerId: String): SecretKey {
        derivedKeys[peerId]?.let { return it }

        val peerPublicKeyBase64 = getPeerPublicKey(peerId) ?: run {
            throw IllegalStateException("No public key for peer")
        }

        val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
        val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

        val keyAgreement = KeyAgreement.getInstance(SecurityConstants.ECDH_ALGORITHM)
        keyAgreement.init(getPrivateKey())
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        val digest = MessageDigest.getInstance(SecurityConstants.SHA_256_ALGORITHM)
        val aesKeyBytes = digest.digest(sharedSecret)
        
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        java.util.Arrays.fill(sharedSecret, 0.toByte())
        java.util.Arrays.fill(aesKeyBytes, 0.toByte())

        derivedKeys[peerId] = aesKey
        return aesKey
    }

    // ────────── AES-256-GCM Encryption ──────────

    fun encrypt(plaintext: String, peerId: String): String {
        val key = deriveSharedKey(peerId)

        val cipher = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv 
        val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + ciphertextWithTag.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String, peerId: String): String? {
        val key = try {
            deriveSharedKey(peerId)
        } catch (e: Exception) {
            return null
        }

        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, SecurityConstants.GCM_IV_LENGTH_BYTES)
            val ciphertextWithTag = combined.copyOfRange(SecurityConstants.GCM_IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
            val spec = GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(ciphertextWithTag)
            val plaintext = String(plainBytes, Charsets.UTF_8)
            java.util.Arrays.fill(plainBytes, 0.toByte())
            plaintext
        } catch (e: Exception) {
            null
        }
    }

    // ────────── Convenience ──────────

    fun encryptOrPassthrough(plaintext: String, peerId: String, requireEncryption: Boolean = false, messageId: String = "", retryCount: Int = 0): Pair<String, Boolean>? {
        if (!hasPeerKey(peerId)) {
            if (requireEncryption) return null
            return plaintext to false
        }
        val encrypted = try {
            encrypt(plaintext, peerId)
        } catch (e: Exception) {
            if (requireEncryption) {
                FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("peerId", peerId.takeLast(8))
                    setCustomKey("messageId", messageId)
                    setCustomKey("retryCount", retryCount)
                    setCustomKey("keyPresent", true)
                    setCustomKey("encryptionMode", "encrypt")
                    recordException(Exception("Encryption failed: ${e.javaClass.simpleName}"))
                }
                return null
            }
            return plaintext to false
        }
        return encrypted to true
    }

    fun decryptOrPassthrough(ciphertext: String, peerId: String): String {
        if (!hasPeerKey(peerId)) return ciphertext
        return decrypt(ciphertext, peerId) ?: ciphertext
    }
}
