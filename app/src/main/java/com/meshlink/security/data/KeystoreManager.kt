package com.meshlink.security.data

interface KeystoreManager {
    @Throws(SecurityRecoveryException::class)
    fun encrypt(plaintext: ByteArray): ByteArray

    @Throws(SecurityRecoveryException::class)
    fun decrypt(ciphertext: ByteArray): ByteArray
}
