package com.meshlink.trust

import android.util.Base64
import java.security.MessageDigest

/**
 * SHA-256 identity fingerprint generator and formatter.
 */
object IdentityFingerprint {

    /**
     * Compute SHA-256 fingerprint from Base64 encoded public key or raw byte array.
     */
    fun compute(publicKeyBase64: String): String {
        return try {
            val keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            compute(keyBytes)
        } catch (e: Exception) {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(publicKeyBase64.toByteArray(Charsets.UTF_8))
            formatHex(hash)
        }
    }

    fun compute(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKeyBytes)
        return formatHex(hash)
    }

    /**
     * Formats byte array into uppercase grouped hex blocks: AB34 CD91 8F20 DAA1 ...
     */
    fun formatHex(bytes: ByteArray): String {
        val hex = bytes.joinToString("") { "%02X".format(it) }
        return hex.chunked(4).joinToString(" ")
    }

    /**
     * Compares two fingerprints ignoring spaces and case.
     */
    fun matches(fingerprintA: String, fingerprintB: String): Boolean {
        val cleanA = fingerprintA.replace(" ", "").uppercase()
        val cleanB = fingerprintB.replace(" ", "").uppercase()
        return cleanA == cleanB
    }
}
