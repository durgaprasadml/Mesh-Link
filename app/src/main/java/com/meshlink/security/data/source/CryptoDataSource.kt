package com.meshlink.security.data.source



interface CryptoDataSource {
    fun generateSalt(): String
    fun generateSaltedHash(input: String, salt: String): String
    fun generateLegacyHash(input: String): String
}
