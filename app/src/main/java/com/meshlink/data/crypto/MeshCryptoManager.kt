package com.meshlink.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import android.os.Build
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
 *
 * Architecture:
 * - Each device generates an ECDH keypair (secp256r1/P-256) stored in Android KeyStore
 * - Public keys are exchanged via KEY_EXCHANGE mesh packets during discovery
 * - Shared secret derived via ECDH → hashed with SHA-256 → AES-256 key
 * - All payloads encrypted with AES-256-GCM (random 12-byte IV per message)
 * - Relay nodes see only encrypted ciphertext — cannot decrypt
 * - Only the intended destination has the matching private key to derive the shared secret
 *
 * Wire format:  Base64( IV[12] + Ciphertext + AuthTag[16] )
 */
@Singleton
class MeshCryptoManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "MeshCrypto"
        private const val KEYSTORE_ALIAS = "mesh_link_ecdh_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_CIPHER = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val PEER_KEYS_PREF = "mesh_peer_keys"
        private const val SELF_PRIVATE_KEY_KEY = "__self_private_key__"
        private const val SELF_PUBLIC_KEY_KEY = "__self_public_key__"
    }

    // In-memory cache of derived AES keys per peer
    private val derivedKeys = mutableMapOf<String, SecretKey>()

    // EncryptedSharedPreferences for persistent peer public key storage
    private val peerKeyStore: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences corruption detected (likely Keystore -30), wiping and retrying: ${e.message}")
            try {
                // Wipe the corrupted preferences file
                context.deleteSharedPreferences(PEER_KEYS_PREF)

                // Also clear the MasterKey from KeyStore to be safe
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (wipeEx: Exception) {
                Log.e(TAG, "Failed to wipe corrupted Keystore/Prefs: ${wipeEx.message}")
            }
            // Re-attempt creation (will generate a fresh MasterKey)
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PEER_KEYS_PREF,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ────────── Key Generation ──────────

    /**
     * Generate our ECDH keypair.
     * Tries Android KeyStore (API 31+) first, falls back to software-backed keys
     * if the hardware/system doesn't support the AGREE_KEY purpose.
     */
    fun getOrCreatePublicKey(): String {
        // 1. Try Keystore if API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return getOrCreateKeystorePublicKey()
            } catch (e: Exception) {
                Log.w(TAG, "Keystore ECDH failed, clearing and falling back: ${e.message}")
                try {
                    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                    keyStore.load(null)
                    keyStore.deleteEntry(KEYSTORE_ALIAS)
                } catch (ignore: Exception) {}
            }
        }

        // 2. Fallback to software-backed keys
        return getOrCreateSoftwarePublicKey()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getOrCreateKeystorePublicKey(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_AGREE_KEY
                )
                    .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                    .build()
            )
            keyPairGenerator.generateKeyPair()
            Log.d(TAG, "Generated new Keystore ECDH keypair")
        }

        val publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    private fun getOrCreateSoftwarePublicKey(): String {
        peerKeyStore.getString(SELF_PUBLIC_KEY_KEY, null)?.let { return it }

        return try {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()

            val pubBase64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
            val privBase64 = Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP)

            peerKeyStore.edit()
                .putString(SELF_PUBLIC_KEY_KEY, pubBase64)
                .putString(SELF_PRIVATE_KEY_KEY, privBase64)
                .apply()

            Log.d(TAG, "Generated new software ECDH keypair")
            pubBase64
        } catch (e: Exception) {
            Log.e(TAG, "Software key generation failed: ${e.message}")
            ""
        }
    }

    /**
     * Retrieve our private key for ECDH agreement.
     */
    private fun getPrivateKey(): java.security.PrivateKey {
        // If we have a software key saved, use it
        val privBase64 = try {
            peerKeyStore.getString(SELF_PRIVATE_KEY_KEY, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read software private key: ${e.message}")
            null
        }

        if (privBase64 != null) {
            val privBytes = Base64.decode(privBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance("EC")
            return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
        }

        // Otherwise, it must be in Keystore
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(KEYSTORE_ALIAS, null) as java.security.PrivateKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve Keystore private key: ${e.message}")
            // Clear corrupted key
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            } catch (ignore: Exception) {}
            throw e
        }
    }

    // ────────── Peer Key Management ──────────

    /**
     * Store a peer's public key (received via KEY_EXCHANGE packet).
     */
    fun storePeerPublicKey(peerId: String, publicKeyBase64: String) {
        peerKeyStore.edit().putString(peerId, publicKeyBase64).apply()
        // Invalidate cached derived key so it gets re-derived with new public key
        derivedKeys.remove(peerId)
        Log.d(TAG, "Stored public key for peer: ${peerId.takeLast(8)}")
    }

    /**
     * Get a peer's stored public key.
     */
    fun getPeerPublicKey(peerId: String): String? {
        return peerKeyStore.getString(peerId, null)
    }

    /**
     * Check if we have a peer's public key (i.e., encryption is available for them).
     */
    fun hasPeerKey(peerId: String): Boolean {
        return peerKeyStore.contains(peerId)
    }

    // ────────── ECDH Key Agreement ──────────

    /**
     * Derive a shared AES-256 key from our private key + peer's public key.
     * Uses ECDH key agreement → SHA-256 hash → AES-256 key.
     */
    private fun deriveSharedKey(peerId: String): SecretKey? {
        // Check cache first
        derivedKeys[peerId]?.let { return it }

        val peerPublicKeyBase64 = getPeerPublicKey(peerId) ?: run {
            Log.w(TAG, "No public key for peer: ${peerId.takeLast(8)}")
            return null
        }

        return try {
            // Decode peer's public key
            val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance("EC")
            val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

            // Perform ECDH key agreement
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(getPrivateKey())
            keyAgreement.doPhase(peerPublicKey, true)
            val sharedSecret = keyAgreement.generateSecret()

            // Hash shared secret → AES-256 key (32 bytes)
            val digest = MessageDigest.getInstance("SHA-256")
            val aesKeyBytes = digest.digest(sharedSecret)
            val aesKey = SecretKeySpec(aesKeyBytes, "AES")

            // Cache for performance
            derivedKeys[peerId] = aesKey
            Log.d(TAG, "Derived AES-256 key for peer: ${peerId.takeLast(8)}")
            aesKey
        } catch (e: Exception) {
            Log.e(TAG, "Key derivation failed for ${peerId.takeLast(8)}: ${e.message}")
            null
        }
    }

    // ────────── AES-256-GCM Encryption ──────────

    /**
     * Encrypt a plaintext payload for a specific peer.
     * Returns Base64( IV[12] + Ciphertext + AuthTag[16] ), or null if encryption fails.
     */
    fun encrypt(plaintext: String, peerId: String): String? {
        val key = deriveSharedKey(peerId) ?: return null

        return try {
            val cipher = Cipher.getInstance(AES_GCM_CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv // Auto-generated 12-byte IV
            val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Concatenate: IV + ciphertext + tag
            val combined = ByteArray(iv.size + ciphertextWithTag.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}")
            null
        }
    }

    /**
     * Decrypt a ciphertext payload from a specific peer.
     * Input: Base64( IV[12] + Ciphertext + AuthTag[16] )
     * Returns the decrypted plaintext, or null if decryption fails.
     */
    fun decrypt(ciphertext: String, peerId: String): String? {
        val key = deriveSharedKey(peerId) ?: return null

        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)

            // Extract IV
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertextWithTag = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(AES_GCM_CIPHER)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(ciphertextWithTag)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed from ${peerId.takeLast(8)}: ${e.message}")
            null
        }
    }

    // ────────── Convenience ──────────

    /**
     * Try to encrypt; if peer key not available, return plaintext as fallback.
     * This ensures messages still work before key exchange completes.
     */
    fun encryptOrPassthrough(plaintext: String, peerId: String): Pair<String, Boolean> {
        if (!hasPeerKey(peerId)) return plaintext to false
        val encrypted = encrypt(plaintext, peerId) ?: return plaintext to false
        return encrypted to true
    }

    /**
     * Try to decrypt; if it fails (not encrypted or wrong key), return ciphertext as-is.
     */
    fun decryptOrPassthrough(ciphertext: String, peerId: String): String {
        if (!hasPeerKey(peerId)) return ciphertext
        return decrypt(ciphertext, peerId) ?: ciphertext
    }
}
