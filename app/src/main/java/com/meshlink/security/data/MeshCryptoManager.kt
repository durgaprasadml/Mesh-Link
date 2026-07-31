package com.meshlink.security.data

import android.content.Context
import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshError
import com.meshlink.domain.model.MeshResult
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
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
 * Production Security & E2E Encryption Engine.
 * Delegates key isolation, storage, and zeroing to KeyManager.
 * Enforces AES-256-GCM authenticated payload encryption.
 */
@Singleton
class MeshCryptoManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val keyManager: KeyManager
) : com.meshlink.security.api.CryptoProvider {

    companion object {
        private const val TAG = "MeshCryptoManager"
    }

    private val encryptCipherLocal = object : ThreadLocal<Cipher>() {
        override fun initialValue() = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
    }
    private val decryptCipherLocal = object : ThreadLocal<Cipher>() {
        override fun initialValue() = Cipher.getInstance(SecurityConstants.AES_GCM_CIPHER)
    }

    // ────────── Identity & Public Key Delegation ──────────

    fun getOrCreatePublicKey(): String = keyManager.getOrCreatePublicKey()

    fun getOrCreateSigningKey(): String = keyManager.getOrCreateSigningKey()

    fun storePeerPublicKey(peerId: String, publicKeyBase64: String) {
        keyManager.storePeerPublicKey(peerId, publicKeyBase64)
    }

    fun getPeerPublicKey(peerId: String): String? = keyManager.getPeerPublicKey(peerId)

    fun hasPeerKey(peerId: String): Boolean = keyManager.hasPeerKey(peerId)

    fun storePeerSigningKey(peerId: String, publicKeyBase64: String) {
        keyManager.storePeerSigningKey(peerId, publicKeyBase64)
    }

    fun getPeerSigningKey(peerId: String): String? = keyManager.getPeerSigningKey(peerId)

    fun rotateIdentityKeys() {
        keyManager.rotateIdentityKeys()
    }

    fun deleteIdentityKeys() {
        keyManager.deleteIdentityKeys()
    }

    fun exportIdentity(): String {
        return keyManager.exportIdentity()
    }

    fun importIdentity(identityBackupBase64: String) {
        keyManager.importIdentity(identityBackupBase64)
    }

    // ────────── Digital Signatures & Fingerprints ──────────

    fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(SecurityConstants.SIGNATURE_ALGORITHM)
        signature.initSign(keyManager.getSigningPrivateKey())
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
            val verified = signature.verify(signatureBytes)
            keyManager.zeroMemory(peerKeyBytes)
            verified
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceFingerprint(publicKeyBase64: String): String {
        return try {
            val pubBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val digest = MessageDigest.getInstance(SecurityConstants.SHA_256_ALGORITHM)
            val hash = digest.digest(pubBytes)
            val fingerprint = hash.joinToString(":") { String.format("%02X", it) }
            keyManager.zeroMemory(pubBytes, hash)
            fingerprint
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun getLocalFingerprint(): String {
        return getDeviceFingerprint(getOrCreateSigningKey())
    }

    // ────────── Ephemeral ECDH Rekey (Forward Secrecy) ──────────

    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(SecurityConstants.EC_ALGORITHM)
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun deriveEphemeralSharedKey(peerId: String, peerPublicKeyBase64: String, myEphemeralPrivateKey: PrivateKey) {
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

        keyManager.setEphemeralSessionKey(peerId, aesKey)
        keyManager.zeroMemory(sharedSecret, aesKeyBytes, peerKeyBytes)
    }

    fun clearPreviousSharedKey(peerId: String) {
        keyManager.clearPreviousSessionKey(peerId)
    }

    fun removeSharedKey(peerId: String) {
        keyManager.removeSessionKey(peerId)
    }

    // ────────── AES-256-GCM 1:1 Session Encryption ──────────

    fun encrypt(plaintext: String, peerId: String, aad: ByteArray? = null): String {
        val key = keyManager.deriveSessionKey(peerId)

        val cipher = encryptCipherLocal.get()!!
        val iv = ByteArray(SecurityConstants.GCM_IV_LENGTH_BYTES)
        java.security.SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        if (aad != null) {
            cipher.updateAAD(aad)
        }

        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertextWithTag = cipher.doFinal(plainBytes)

        val combined = ByteArray(iv.size + ciphertextWithTag.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)

        keyManager.zeroMemory(plainBytes)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String, peerId: String, aad: ByteArray? = null, usePreviousKey: Boolean = false): String? {
        val key = try {
            if (usePreviousKey) {
                keyManager.getPreviousSessionKey(peerId) ?: return null
            } else {
                keyManager.deriveSessionKey(peerId)
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
            keyManager.zeroMemory(plainBytes, combined)
            plaintext
        } catch (e: Exception) {
            null
        }
    }

    // ────────── Versioned Broadcast Encryption ──────────

    fun encryptBroadcast(plaintext: String, version: Int = keyManager.getCurrentBroadcastKeyVersion()): Pair<String, Int> {
        val key = keyManager.getBroadcastKey(version)
            ?: throw IllegalStateException("Broadcast key v$version not found")

        val cipher = encryptCipherLocal.get()!!
        val iv = ByteArray(SecurityConstants.GCM_IV_LENGTH_BYTES)
        java.security.SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val aadBytes = "bcast_v$version".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aadBytes)

        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertextWithTag = cipher.doFinal(plainBytes)

        val combined = ByteArray(iv.size + ciphertextWithTag.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)

        keyManager.zeroMemory(plainBytes, aadBytes)
        return Base64.encodeToString(combined, Base64.NO_WRAP) to version
    }

    fun decryptBroadcast(ciphertext: String, version: Int): String? {
        val key = keyManager.getBroadcastKey(version) ?: return null

        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, SecurityConstants.GCM_IV_LENGTH_BYTES)
            val ciphertextWithTag = combined.copyOfRange(SecurityConstants.GCM_IV_LENGTH_BYTES, combined.size)

            val cipher = decryptCipherLocal.get()!!
            val spec = GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val aadBytes = "bcast_v$version".toByteArray(Charsets.UTF_8)
            cipher.updateAAD(aadBytes)

            val plainBytes = cipher.doFinal(ciphertextWithTag)
            val plaintext = String(plainBytes, Charsets.UTF_8)
            keyManager.zeroMemory(plainBytes, combined, aadBytes)
            plaintext
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentBroadcastKeyVersion(): Int = keyManager.getCurrentBroadcastKeyVersion()
    fun rotateBroadcastKey(): Int = keyManager.rotateBroadcastKey()

    // ────────── Convenience Passthrough Methods ──────────

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
                MeshLogger.e(TAG, "Encryption failed: ${e.message}")
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

    fun isHardwareKeystoreUsed(): Boolean = keyManager.isHardwareKeystoreUsed()

    override suspend fun encrypt(data: ByteArray, peerId: String): ByteArray {
        val payload = String(data, Charsets.UTF_8)
        val enc = encrypt(payload, peerId)
        return enc.toByteArray(Charsets.UTF_8)
    }

    override suspend fun encryptData(peerId: String, data: ByteArray): MeshResult<ByteArray> {
        return try {
            val payload = String(data, Charsets.UTF_8)
            val enc = encrypt(payload, peerId)
            MeshResult.Success(enc.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            MeshResult.Error(MeshError.SecurityError("Encryption failed", peerId, e))
        }
    }

    override suspend fun decrypt(data: ByteArray, peerId: String): ByteArray {
        val ciphertext = String(data, Charsets.UTF_8)
        val dec = decryptOrPassthrough(ciphertext, peerId)
        if (dec == ciphertext && !ciphertext.startsWith("{")) throw Exception("Decryption failed")
        return dec.toByteArray(Charsets.UTF_8)
    }

    override suspend fun decryptData(peerId: String, data: ByteArray): MeshResult<ByteArray> {
        return try {
            val ciphertext = String(data, Charsets.UTF_8)
            val dec = decryptOrPassthrough(ciphertext, peerId)
            if (dec == ciphertext && !ciphertext.startsWith("{")) throw Exception("Decryption failed")
            MeshResult.Success(dec.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            MeshResult.Error(MeshError.SecurityError("Decryption failed", peerId, e))
        }
    }
}
