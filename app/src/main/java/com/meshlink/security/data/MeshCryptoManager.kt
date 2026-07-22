package com.meshlink.security.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

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
    private val previousDerivedKeys = java.util.concurrent.ConcurrentHashMap<String, SecretKey>()

    private val encryptCipherLocal = object : ThreadLocal<Cipher>() {
        override fun initialValue() = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
    }
    private val decryptCipherLocal = object : ThreadLocal<Cipher>() {
        override fun initialValue() = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
    }

    // EncryptedSharedPreferences for persistent peer public key storage
    private val peerKeyStore: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            val msg = e.javaClass.simpleName
            MeshLogger.e(TAG, "EncryptedSharedPreferences corruption detected, wiping and retrying: $msg")
            try {
                context.deleteSharedPreferences(SecurityConstants.PEER_KEYS_PREF)
                val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (wipeEx: Exception) {
                MeshLogger.e(TAG, "Failed to wipe corrupted Keystore/Prefs: ${wipeEx.javaClass.simpleName}")
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

    private val peerSigningKeyStore: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SecurityConstants.PEER_SIGNING_KEYS_PREF,
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
                MeshLogger.w(TAG, "Keystore ECDH failed, clearing and falling back: ${e.javaClass.simpleName}")
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
            MeshLogger.e(TAG, "Software key generation failed: ${e.javaClass.simpleName}")
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

    // ────────── Identity & Signing (Phase A2.1) ──────────

    fun getOrCreateSigningKey(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return getOrCreateKeystoreSigningKey()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Keystore signing key failed, falling back: ${e.javaClass.simpleName}")
            }
        }
        return getOrCreateSoftwareSigningKey()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getOrCreateKeystoreSigningKey(): String {
        val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(SecurityConstants.SIGNING_KEYSTORE_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                SecurityConstants.ANDROID_KEYSTORE
            )
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    SecurityConstants.SIGNING_KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                    .build()
            )
            keyPairGenerator.generateKeyPair()
        }

        val publicKey = keyStore.getCertificate(SecurityConstants.SIGNING_KEYSTORE_ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    private fun getOrCreateSoftwareSigningKey(): String {
        peerSigningKeyStore.getString(SecurityConstants.SELF_SIGNING_PUBLIC_KEY_KEY, null)?.let { return it }

        return try {
            val kpg = KeyPairGenerator.getInstance(SecurityConstants.EC_ALGORITHM)
            kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()

            val pubBase64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
            val privBase64 = Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP)

            peerSigningKeyStore.edit()
                .putString(SecurityConstants.SELF_SIGNING_PUBLIC_KEY_KEY, pubBase64)
                .putString(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY, privBase64)
                .apply()

            pubBase64
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Software signing key generation failed: ${e.javaClass.simpleName}")
            ""
        }
    }

    private fun getSigningPrivateKey(): java.security.PrivateKey {
        val privBase64 = try {
            peerSigningKeyStore.getString(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY, null)
        } catch (e: Exception) {
            null
        }

        if (privBase64 != null) {
            val privBytes = Base64.decode(privBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
            return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
        }

        return try {
            val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(SecurityConstants.SIGNING_KEYSTORE_ALIAS, null) as java.security.PrivateKey
        } catch (e: Exception) {
            throw e
        }
    }

    fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(SecurityConstants.SIGNATURE_ALGORITHM)
        signature.initSign(getSigningPrivateKey())
        signature.update(data)
        return signature.sign()
    }

    fun verifySignature(publicKeyBase64: String, data: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val peerKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
            val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

            val signature = Signature.getInstance(SecurityConstants.SIGNATURE_ALGORITHM)
            signature.initVerify(peerPublicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceFingerprint(publicKeyBase64: String): String {
        return try {
            val pubBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val digest = MessageDigest.getInstance(SecurityConstants.SHA_256_ALGORITHM)
            val hash = digest.digest(pubBytes)
            hash.joinToString(":") { String.format("%02X", it) }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun getLocalFingerprint(): String {
        return getDeviceFingerprint(getOrCreateSigningKey())
    }

    fun storePeerSigningKey(peerId: String, publicKeyBase64: String) {
        peerSigningKeyStore.edit().putString(peerId, publicKeyBase64).apply()
    }

    fun getPeerSigningKey(peerId: String): String? {
        return peerSigningKeyStore.getString(peerId, null)
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
        return derivedKeys.computeIfAbsent(peerId) {
            val peerPublicKeyBase64 = getPeerPublicKey(peerId)
            if (peerPublicKeyBase64 == null) {
                com.meshlink.common.logger.MeshLogger.w("MeshCryptoManager", "No public key for peer $peerId")
                return@computeIfAbsent SecretKeySpec(ByteArray(32), SecurityConstants.AES_GCM_CIPHER)
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

            aesKey
        }
    }

    // ────────── Ephemeral ECDH Rekey (Phase A2.3) ──────────

    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(SecurityConstants.EC_ALGORITHM)
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun deriveEphemeralSharedKey(peerId: String, peerPublicKeyBase64: String, myEphemeralPrivateKey: java.security.PrivateKey) {
        val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
        val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

        val keyAgreement = KeyAgreement.getInstance(SecurityConstants.ECDH_ALGORITHM)
        keyAgreement.init(myEphemeralPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        val digest = MessageDigest.getInstance(SecurityConstants.SHA_256_ALGORITHM)
        val aesKeyBytes = digest.digest(sharedSecret)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        java.util.Arrays.fill(sharedSecret, 0.toByte())
        java.util.Arrays.fill(aesKeyBytes, 0.toByte())

        // Rotate keys
        val oldKey = derivedKeys[peerId]
        if (oldKey != null) {
            previousDerivedKeys[peerId] = oldKey
        }
        derivedKeys[peerId] = aesKey
    }

    fun clearPreviousSharedKey(peerId: String) {
        previousDerivedKeys.remove(peerId)
    }

    // ────────── AES-256-GCM Encryption ──────────

    fun encrypt(plaintext: String, peerId: String, aad: ByteArray? = null): String {
        val key = deriveSharedKey(peerId)

        val cipher = encryptCipherLocal.get()!!
        val iv = ByteArray(SecurityConstants.GCM_IV_LENGTH_BYTES)
        java.security.SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        if (aad != null) {
            cipher.updateAAD(aad)
        }

        val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + ciphertextWithTag.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String, peerId: String, aad: ByteArray? = null, usePreviousKey: Boolean = false): String? {
        val key = try {
            if (usePreviousKey) {
                previousDerivedKeys[peerId] ?: return null
            } else {
                deriveSharedKey(peerId)
            }
        } catch (e: Exception) {
            return null
        }

        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, SecurityConstants.GCM_IV_LENGTH_BYTES)
            val ciphertextWithTag = combined.copyOfRange(SecurityConstants.GCM_IV_LENGTH_BYTES, combined.size)

            val cipher = decryptCipherLocal.get()!!
            val spec = GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            if (aad != null) {
                cipher.updateAAD(aad)
            }

            val plainBytes = cipher.doFinal(ciphertextWithTag)
            val plaintext = String(plainBytes, Charsets.UTF_8)
            java.util.Arrays.fill(plainBytes, 0.toByte())
            plaintext
        } catch (e: Exception) {
            null
        }
    }

    // ────────── Convenience ──────────
    fun encryptOrPassthrough(
        plaintext: String,
        peerId: String,
        requireEncryption: Boolean = false,
        messageId: String = "",
        retryCount: Int = 0,
        aad: ByteArray? = null
    ): Pair<String, Boolean>? {
        if (!hasPeerKey(peerId)) {
            if (requireEncryption) return null
            return plaintext to false
        }
        val encrypted = try {
            encrypt(plaintext, peerId, aad)
        } catch (e: Exception) {
            if (requireEncryption) {
                FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("peerId", com.meshlink.util.MeshIdNormalizer.canonicalize(peerId))
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

    fun decryptOrPassthrough(ciphertext: String, peerId: String, aad: ByteArray? = null, usePreviousKey: Boolean = false): String {
        if (!hasPeerKey(peerId)) return ciphertext
        return decrypt(ciphertext, peerId, aad, usePreviousKey) ?: ciphertext
    }

    fun removeSharedKey(peerId: String) {
        derivedKeys.remove(peerId)
        previousDerivedKeys.remove(peerId)
    }

    // ────────── Identity Management ──────────

    fun rotateIdentityKeys() {
        val now = System.currentTimeMillis()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(SecurityConstants.MESH_KEYSTORE_ALIAS)
                keyStore.deleteEntry(SecurityConstants.SIGNING_KEYSTORE_ALIAS)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to delete Keystore entries during rotation: ${e.message}")
        }
        
        peerKeyStore.edit()
            .remove(SecurityConstants.SELF_PUBLIC_KEY_KEY)
            .remove(SecurityConstants.SELF_PRIVATE_KEY_KEY)
            .putLong(SecurityConstants.LAST_ROTATION_TIME, now)
            .apply()
            
        peerSigningKeyStore.edit()
            .remove(SecurityConstants.SELF_SIGNING_PUBLIC_KEY_KEY)
            .remove(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY)
            .apply()
            
        // Pre-generate new keys
        getOrCreatePublicKey()
        getOrCreateSigningKey()
    }

    fun deleteIdentityKeys() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(SecurityConstants.MESH_KEYSTORE_ALIAS)
                keyStore.deleteEntry(SecurityConstants.SIGNING_KEYSTORE_ALIAS)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to delete Keystore entries: ${e.message}")
        }
        
        peerKeyStore.edit()
            .remove(SecurityConstants.SELF_PUBLIC_KEY_KEY)
            .remove(SecurityConstants.SELF_PRIVATE_KEY_KEY)
            .remove(SecurityConstants.KEY_CREATION_TIME)
            .remove(SecurityConstants.LAST_ROTATION_TIME)
            .apply()
            
        peerSigningKeyStore.edit()
            .remove(SecurityConstants.SELF_SIGNING_PUBLIC_KEY_KEY)
            .remove(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY)
            .apply()
    }

    fun exportIdentity(): String {
        // Can only export software keys
        val pubKey = peerKeyStore.getString(SecurityConstants.SELF_PUBLIC_KEY_KEY, null)
        val privKey = peerKeyStore.getString(SecurityConstants.SELF_PRIVATE_KEY_KEY, null)
        val signPubKey = peerSigningKeyStore.getString(SecurityConstants.SELF_SIGNING_PUBLIC_KEY_KEY, null)
        val signPrivKey = peerSigningKeyStore.getString(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY, null)
        
        if (pubKey == null || privKey == null || signPubKey == null || signPrivKey == null) {
            throw IllegalStateException("Cannot export hardware-backed identities or missing keys.")
        }
        
        val json = org.json.JSONObject()
        json.put("pub", pubKey)
        json.put("priv", privKey)
        json.put("sign_pub", signPubKey)
        json.put("sign_priv", signPrivKey)
        return Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun importIdentity(identityBackupBase64: String) {
        try {
            val jsonString = String(Base64.decode(identityBackupBase64, Base64.NO_WRAP), Charsets.UTF_8)
            val json = org.json.JSONObject(jsonString)
            val pub = json.getString("pub")
            val priv = json.getString("priv")
            val signPub = json.getString("sign_pub")
            val signPriv = json.getString("sign_priv")
            
            // Delete existing hardware keys if present
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                    keyStore.load(null)
                    keyStore.deleteEntry(SecurityConstants.MESH_KEYSTORE_ALIAS)
                    keyStore.deleteEntry(SecurityConstants.SIGNING_KEYSTORE_ALIAS)
                }
            } catch (ignore: Exception) {}
            
            peerKeyStore.edit()
                .putString(SecurityConstants.SELF_PUBLIC_KEY_KEY, pub)
                .putString(SecurityConstants.SELF_PRIVATE_KEY_KEY, priv)
                .apply()
                
            peerSigningKeyStore.edit()
                .putString(SecurityConstants.SELF_SIGNING_PUBLIC_KEY_KEY, signPub)
                .putString(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY, signPriv)
                .apply()
                
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to import identity: ${e.message}")
            throw IllegalArgumentException("Invalid identity backup format")
        }
    }

    fun getKeyCreationTime(): Long {
        val time = peerKeyStore.getLong(SecurityConstants.KEY_CREATION_TIME, 0L)
        if (time == 0L) {
            val now = System.currentTimeMillis()
            peerKeyStore.edit().putLong(SecurityConstants.KEY_CREATION_TIME, now).apply()
            return now
        }
        return time
    }

    fun getLastRotationTime(): Long {
        return peerKeyStore.getLong(SecurityConstants.LAST_ROTATION_TIME, getKeyCreationTime())
    }
}
