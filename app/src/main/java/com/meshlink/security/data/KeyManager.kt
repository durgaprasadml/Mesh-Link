package com.meshlink.security.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated KeyManager responsible for cryptographic key isolation, versioning,
 * storage, and secure memory hygiene.
 *
 * Maintains 3 distinct key types:
 * 1. Identity Keys: Hardware-backed EC signing keys (never used for symmetric payload encryption).
 * 2. Session Keys: ECDH derived ephemeral symmetric keys bound to peer sessions.
 * 3. Broadcast Group Keys: Versioned network shared symmetric keys for broadcast encryption.
 */
@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "KeyManager"
        private const val BROADCAST_KEY_PREF = "mesh_broadcast_keys"
        private const val BROADCAST_KEY_PREFIX = "__broadcast_key_v"
        private const val CURRENT_BROADCAST_VERSION_KEY = "__current_broadcast_version__"
    }

    // In-memory cache of derived session AES keys: peerId -> SecretKey
    private val sessionKeys = ConcurrentHashMap<String, SecretKey>()
    private val previousSessionKeys = ConcurrentHashMap<String, SecretKey>()
    
    // Broadcast Group Keys: keyVersion -> SecretKey
    private val broadcastKeys = ConcurrentHashMap<Int, SecretKey>()
    private var currentBroadcastVersion: Int = 1

    private val peerKeyStore: SharedPreferences by lazy {
        createEncryptedPrefs(SecurityConstants.PEER_KEYS_PREF)
    }

    private val peerSigningKeyStore: SharedPreferences by lazy {
        createEncryptedPrefs(SecurityConstants.PEER_SIGNING_KEYS_PREF)
    }

    private val broadcastKeyStore: SharedPreferences by lazy {
        createEncryptedPrefs(BROADCAST_KEY_PREF)
    }

    init {
        loadOrInitializeBroadcastKey()
    }

    private fun createEncryptedPrefs(prefName: String): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                prefName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to create EncryptedSharedPreferences ($prefName), clearing and retrying: ${e.message}")
            try {
                context.deleteSharedPreferences(prefName)
            } catch (_: Exception) {}
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                prefName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    // ────────── Memory Zeroing Hygiene ──────────

    fun zeroMemory(vararg arrays: ByteArray?) {
        for (array in arrays) {
            if (array != null) {
                Arrays.fill(array, 0.toByte())
            }
        }
    }

    // ────────── Identity Key Management ──────────

    fun getOrCreatePublicKey(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return getOrCreateKeystorePublicKey()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Keystore ECDH key creation failed, falling back: ${e.message}")
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
            val encodedPriv = kp.private.encoded
            val privBase64 = Base64.encodeToString(encodedPriv, Base64.NO_WRAP)

            peerKeyStore.edit()
                .putString(SecurityConstants.SELF_PUBLIC_KEY_KEY, pubBase64)
                .putString(SecurityConstants.SELF_PRIVATE_KEY_KEY, privBase64)
                .apply()

            zeroMemory(encodedPriv)
            pubBase64
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Software key generation failed: ${e.message}")
            ""
        }
    }

    fun getPrivateKey(): PrivateKey {
        val privBase64 = try {
            peerKeyStore.getString(SecurityConstants.SELF_PRIVATE_KEY_KEY, null)
        } catch (_: Exception) {
            null
        }

        if (privBase64 != null) {
            val privBytes = Base64.decode(privBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            zeroMemory(privBytes)
            return privateKey
        }

        return try {
            val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(SecurityConstants.MESH_KEYSTORE_ALIAS, null) as PrivateKey
        } catch (e: Exception) {
            throw e
        }
    }

    fun getOrCreateSigningKey(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return getOrCreateKeystoreSigningKey()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Keystore signing key failed, falling back: ${e.message}")
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
            MeshLogger.e(TAG, "Software signing key generation failed: ${e.message}")
            ""
        }
    }

    fun getSigningPrivateKey(): PrivateKey {
        val privBase64 = try {
            peerSigningKeyStore.getString(SecurityConstants.SELF_SIGNING_PRIVATE_KEY_KEY, null)
        } catch (_: Exception) {
            null
        }

        if (privBase64 != null) {
            val privBytes = Base64.decode(privBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            zeroMemory(privBytes)
            return privateKey
        }

        return try {
            val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(SecurityConstants.SIGNING_KEYSTORE_ALIAS, null) as PrivateKey
        } catch (e: Exception) {
            throw e
        }
    }

    fun storePeerPublicKey(peerId: String, publicKeyBase64: String) {
        peerKeyStore.edit().putString(peerId, publicKeyBase64).apply()
        sessionKeys.remove(peerId)
    }

    fun getPeerPublicKey(peerId: String): String? {
        return peerKeyStore.getString(peerId, null)
    }

    fun hasPeerKey(peerId: String): Boolean {
        return peerKeyStore.contains(peerId)
    }

    fun storePeerSigningKey(peerId: String, publicKeyBase64: String) {
        peerSigningKeyStore.edit().putString(peerId, publicKeyBase64).apply()
    }

    fun getPeerSigningKey(peerId: String): String? {
        return peerSigningKeyStore.getString(peerId, null)
    }

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

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val keyStore = KeyStore.getInstance(SecurityConstants.ANDROID_KEYSTORE)
                    keyStore.load(null)
                    keyStore.deleteEntry(SecurityConstants.MESH_KEYSTORE_ALIAS)
                    keyStore.deleteEntry(SecurityConstants.SIGNING_KEYSTORE_ALIAS)
                }
            } catch (_: Exception) {}

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

    // ────────── Session Key Management ──────────

    fun deriveSessionKey(peerId: String): SecretKey {
        return sessionKeys.computeIfAbsent(peerId) {
            val peerPublicKeyBase64 = getPeerPublicKey(peerId)
                ?: return@computeIfAbsent SecretKeySpec(ByteArray(32), "AES")

            val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(SecurityConstants.EC_ALGORITHM)
            val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

            val keyAgreement = KeyAgreement.getInstance(SecurityConstants.ECDH_ALGORITHM)
            keyAgreement.init(getPrivateKey())
            keyAgreement.doPhase(peerPublicKey, true)
            val sharedSecret = keyAgreement.generateSecret()

            val digest = java.security.MessageDigest.getInstance(SecurityConstants.SHA_256_ALGORITHM)
            val aesKeyBytes = digest.digest(sharedSecret)

            val aesKey = SecretKeySpec(aesKeyBytes, "AES")

            zeroMemory(sharedSecret, aesKeyBytes, peerKeyBytes)

            aesKey
        }
    }

    fun setEphemeralSessionKey(peerId: String, key: SecretKey) {
        val old = sessionKeys[peerId]
        if (old != null) {
            previousSessionKeys[peerId] = old
        }
        sessionKeys[peerId] = key
    }

    fun getPreviousSessionKey(peerId: String): SecretKey? {
        return previousSessionKeys[peerId]
    }

    fun clearPreviousSessionKey(peerId: String) {
        previousSessionKeys.remove(peerId)
    }

    fun removeSessionKey(peerId: String) {
        sessionKeys.remove(peerId)
        previousSessionKeys.remove(peerId)
    }

    // ────────── Broadcast Group Key Management ──────────

    fun getCurrentBroadcastKeyVersion(): Int = currentBroadcastVersion

    fun getBroadcastKey(version: Int = currentBroadcastVersion): SecretKey? {
        return broadcastKeys[version]
    }

    private fun loadOrInitializeBroadcastKey() {
        var version = broadcastKeyStore.getInt(CURRENT_BROADCAST_VERSION_KEY, 1)
        var keyBase64 = broadcastKeyStore.getString("$BROADCAST_KEY_PREFIX$version", null)

        if (keyBase64 == null) {
            val rawKey = ByteArray(32)
            SecureRandom().nextBytes(rawKey)
            keyBase64 = Base64.encodeToString(rawKey, Base64.NO_WRAP)
            broadcastKeyStore.edit()
                .putInt(CURRENT_BROADCAST_VERSION_KEY, version)
                .putString("$BROADCAST_KEY_PREFIX$version", keyBase64)
                .apply()
            zeroMemory(rawKey)
        }

        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        currentBroadcastVersion = version
        broadcastKeys[version] = SecretKeySpec(keyBytes, "AES")
        zeroMemory(keyBytes)
    }

    fun rotateBroadcastKey(): Int {
        val nextVersion = currentBroadcastVersion + 1
        val rawKey = ByteArray(32)
        SecureRandom().nextBytes(rawKey)
        val keyBase64 = Base64.encodeToString(rawKey, Base64.NO_WRAP)

        broadcastKeyStore.edit()
            .putInt(CURRENT_BROADCAST_VERSION_KEY, nextVersion)
            .putString("$BROADCAST_KEY_PREFIX$nextVersion", keyBase64)
            .apply()

        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        broadcastKeys[nextVersion] = SecretKeySpec(keyBytes, "AES")
        currentBroadcastVersion = nextVersion
        zeroMemory(rawKey, keyBytes)

        MeshLogger.d(TAG, "Rotated Mesh Broadcast Key to version $nextVersion")
        return nextVersion
    }

    fun importBroadcastKey(version: Int, keyBase64: String) {
        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        broadcastKeys[version] = SecretKeySpec(keyBytes, "AES")
        if (version > currentBroadcastVersion) {
            currentBroadcastVersion = version
            broadcastKeyStore.edit().putInt(CURRENT_BROADCAST_VERSION_KEY, version).apply()
        }
        broadcastKeyStore.edit().putString("$BROADCAST_KEY_PREFIX$version", keyBase64).apply()
        zeroMemory(keyBytes)
    }

    fun isHardwareKeystoreUsed(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    fun clearAllKeys() {
        sessionKeys.clear()
        previousSessionKeys.clear()
        peerKeyStore.edit().clear().apply()
        peerSigningKeyStore.edit().clear().apply()
        broadcastKeyStore.edit().clear().apply()
    }
}
