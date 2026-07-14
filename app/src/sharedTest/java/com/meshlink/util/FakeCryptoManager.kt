package com.meshlink.util

class FakeCryptoManager {
    fun encrypt(data: String): String = "encrypted_$data"
    fun decrypt(data: String): String = data.removePrefix("encrypted_")
    fun generateKeyPair() {}
}
