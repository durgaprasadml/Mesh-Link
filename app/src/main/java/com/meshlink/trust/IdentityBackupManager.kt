package com.meshlink.trust

import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedBackupPayload(
    val version: Int = 1,
    val saltBase64: String,
    val ivBase64: String,
    val encryptedDataBase64: String
) {
    fun toJson(): String {
        return """{"version":$version,"salt":"$saltBase64","iv":"$ivBase64","data":"$encryptedDataBase64"}"""
    }

    companion object {
        fun fromJson(jsonStr: String): EncryptedBackupPayload {
            val version = extractValue(jsonStr, "version").toIntOrNull() ?: 1
            val salt = extractValue(jsonStr, "salt")
            val iv = extractValue(jsonStr, "iv")
            val data = extractValue(jsonStr, "data")

            return EncryptedBackupPayload(
                version = version,
                saltBase64 = salt,
                ivBase64 = iv,
                encryptedDataBase64 = data
            )
        }

        private fun extractValue(json: String, key: String): String {
            val pattern = """"$key"\s*:\s*("(.*?)"|(\d+))""".toRegex()
            val match = pattern.find(json) ?: return ""
            return match.groupValues[2].ifEmpty { match.groupValues[3] }
        }
    }
}

/**
 * Manager for secure offline identity backup and restoration.
 */
@Singleton
class IdentityBackupManager @Inject constructor(
    private val identityManager: MeshIdentityManager,
    private val auditLog: IdentityAuditLog
) {
    companion object {
        private const val TAG = "IdentityBackupManager"
        private const val PBKDF2_ITERATIONS = 10000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    /**
     * Exports local MeshIdentity into encrypted backup string using passphrase.
     */
    fun exportBackup(passphrase: String): String {
        require(passphrase.length >= 6) { "Passphrase must be at least 6 characters" }
        val identity = identityManager.getOrCreateIdentity()
        val plainTextJson = identity.toJson()

        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val encryptedBytes = cipher.doFinal(plainTextJson.toByteArray(Charsets.UTF_8))

        val payload = EncryptedBackupPayload(
            saltBase64 = encodeBase64(salt),
            ivBase64 = encodeBase64(iv),
            encryptedDataBase64 = encodeBase64(encryptedBytes)
        )

        auditLog.logEvent(
            eventType = AuditEventType.BACKUP_CREATED,
            meshId = identity.meshId,
            details = "Identity backup successfully created"
        )

        return payload.toJson()
    }

    /**
     * Restores MeshIdentity from encrypted backup string using passphrase.
     */
    fun restoreBackup(encryptedBackupJson: String, passphrase: String): MeshIdentity {
        val payload = EncryptedBackupPayload.fromJson(encryptedBackupJson)
        val salt = decodeBase64(payload.saltBase64)
        val iv = decodeBase64(payload.ivBase64)
        val encryptedBytes = decodeBase64(payload.encryptedDataBase64)

        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        val plainTextJson = String(decryptedBytes, Charsets.UTF_8)

        val identity = MeshIdentity.fromJson(plainTextJson)

        auditLog.logEvent(
            eventType = AuditEventType.RESTORE_COMPLETED,
            meshId = identity.meshId,
            details = "Identity backup successfully restored"
        )

        MeshLogger.i(TAG, "Restored identity ${identity.meshId} from backup")
        return identity
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            Base64.decode(str, Base64.NO_WRAP)
        } catch (e: Exception) {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): javax.crypto.SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
