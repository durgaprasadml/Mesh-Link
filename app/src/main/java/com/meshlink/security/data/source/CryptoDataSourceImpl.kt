package com.meshlink.security.data.source

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

class CryptoDataSourceImpl @Inject constructor() : CryptoDataSource {

    override fun generateSalt(): String {
        val random = java.security.SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
    }

    override fun generateLegacyHash(input: String): String {
        val bytes = input.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override fun generateSaltedHash(input: String, salt: String): String {
        val iterations = 10000
        val keyLength = 256
        val spec = PBEKeySpec(
            input.toCharArray(),
            android.util.Base64.decode(salt, android.util.Base64.NO_WRAP),
            iterations,
            keyLength
        )
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
}
