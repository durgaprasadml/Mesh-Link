package com.meshlink.domain.repository



interface SecurityRepository {
    fun encryptAndWrapPayload(
        plaintext: String,
        targetPeerId: String,
        requireEncryption: Boolean,
        messageId: String
    ): Pair<String, Boolean>?

    suspend fun setAppLockPin(pin: String)
    suspend fun verifyAppLockPin(pin: String): Boolean
    suspend fun hasAppLockPin(): Boolean
    suspend fun clearAppLockPin()
}
