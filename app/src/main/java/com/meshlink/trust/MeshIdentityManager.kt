package com.meshlink.trust

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meshlink.common.logger.MeshLogger
import com.meshlink.security.data.MeshCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe singleton manager for permanent local Mesh Identity.
 * Identity is anchored to UUID + EC Public Key binding.
 */
@Singleton
class MeshIdentityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: MeshCryptoManager
) {
    companion object {
        private const val TAG = "MeshIdentityManager"
        private const val IDENTITY_PREFS = "mesh_identity_prefs"
        private const val KEY_MESH_ID = "mesh_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_CREATION_TIME = "creation_timestamp"
        private const val KEY_IDENTITY_VERSION = "identity_version"
        private const val KEY_SIGNATURE = "identity_signature"
    }

    private val identityCache = AtomicReference<MeshIdentity?>(null)

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "EncryptedSharedPreferences error, clearing identity prefs: ${e.message}")
            context.deleteSharedPreferences(IDENTITY_PREFS)
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            IDENTITY_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Gets or generates permanent local MeshIdentity.
     */
    fun getOrCreateIdentity(): MeshIdentity {
        identityCache.get()?.let { return it }

        synchronized(this) {
            identityCache.get()?.let { return it }

            val savedMeshId = prefs.getString(KEY_MESH_ID, null)
            val rawDisplayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
            val savedDisplayName = if (rawDisplayName == "User" || rawDisplayName == "Device" || rawDisplayName == "Man" || rawDisplayName == "Peer") "" else rawDisplayName
            val savedCreationTime = prefs.getLong(KEY_CREATION_TIME, 0L)
            val savedVersion = prefs.getInt(KEY_IDENTITY_VERSION, 1)
            val savedSignature = prefs.getString(KEY_SIGNATURE, "") ?: ""

            val publicKeyBase64 = cryptoManager.getOrCreatePublicKey()

            if (savedMeshId != null && savedCreationTime > 0L) {
                val identity = MeshIdentity(
                    meshId = savedMeshId,
                    publicKey = publicKeyBase64,
                    identityVersion = savedVersion,
                    creationTimestamp = savedCreationTime,
                    displayName = savedDisplayName,
                    signature = savedSignature
                )
                identityCache.set(identity)
                return identity
            }

            // Generate new permanent Mesh ID from secure random UUID
            val newMeshId = "mesh-" + UUID.randomUUID().toString()
            val creationTime = System.currentTimeMillis()
            val version = 1

            val tempIdentity = MeshIdentity(
                meshId = newMeshId,
                publicKey = publicKeyBase64,
                identityVersion = version,
                creationTimestamp = creationTime,
                displayName = savedDisplayName,
                signature = ""
            )

            // Sign the identity binding using private key
            val signature = signIdentityPayload(tempIdentity.toSigningPayload())
            val finalIdentity = tempIdentity.copy(signature = signature)

            prefs.edit()
                .putString(KEY_MESH_ID, newMeshId)
                .putString(KEY_DISPLAY_NAME, savedDisplayName)
                .putLong(KEY_CREATION_TIME, creationTime)
                .putInt(KEY_IDENTITY_VERSION, version)
                .putString(KEY_SIGNATURE, signature)
                .apply()

            identityCache.set(finalIdentity)
            MeshLogger.i(TAG, "Generated new local Mesh Identity: ${finalIdentity.meshId}")
            return finalIdentity
        }
    }

    /**
     * Updates display name for local identity.
     */
    fun updateDisplayName(newDisplayName: String): MeshIdentity {
        val current = getOrCreateIdentity()
        val updated = current.copy(displayName = newDisplayName.trim())

        synchronized(this) {
            prefs.edit().putString(KEY_DISPLAY_NAME, updated.displayName).apply()
            identityCache.set(updated)
        }
        return updated
    }

    /**
     * Signs payload string with local identity private key.
     */
    fun signIdentityPayload(payload: String): String {
        return try {
            val sigBytes = cryptoManager.sign(payload.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(sigBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to sign identity payload: ${e.message}")
            ""
        }
    }

    /**
     * Verifies signature for remote identity using remote public key.
     */
    fun verifyRemoteIdentity(identity: MeshIdentity): Boolean {
        if (identity.signature.isEmpty()) return false
        return try {
            val sigBytes = Base64.decode(identity.signature, Base64.NO_WRAP)
            val payloadBytes = identity.toSigningPayload().toByteArray(Charsets.UTF_8)
            cryptoManager.verifySignature(identity.publicKey, payloadBytes, sigBytes)
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Remote identity signature verification failed: ${e.message}")
            false
        }
    }
}
