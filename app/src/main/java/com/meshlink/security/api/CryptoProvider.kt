package com.meshlink.security.api

interface CryptoProvider {
    suspend fun encrypt(data: ByteArray, peerId: String): ByteArray
    suspend fun decrypt(data: ByteArray, peerId: String): ByteArray
}
