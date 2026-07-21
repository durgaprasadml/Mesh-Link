package com.meshlink.security.data

import com.meshlink.domain.repository.SecurityRepository
import com.meshlink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    private val settingsRepository: SettingsRepository
) : SecurityRepository {

    override fun encryptAndWrapPayload(
        plaintext: String,
        targetPeerId: String,
        requireEncryption: Boolean,
        messageId: String
    ): Pair<String, Boolean>? {
        return cryptoManager.encryptOrPassthrough(plaintext, targetPeerId, requireEncryption, messageId)
    }
}
