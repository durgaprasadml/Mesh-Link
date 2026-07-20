package com.meshlink.security.data

import com.meshlink.domain.repository.SecurityRepository
import com.meshlink.domain.repository.SettingsRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

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

    override suspend fun setAppLockPin(pin: String) {
        val hash = hashPin(pin)
        settingsRepository.setAppLockPinHash(hash)
    }

    override suspend fun verifyAppLockPin(pin: String): Boolean {
        val storedHash = settingsRepository.appLockPinHash.first() ?: return false
        return storedHash == hashPin(pin)
    }

    override suspend fun hasAppLockPin(): Boolean {
        return settingsRepository.appLockPinHash.first() != null
    }

    override suspend fun clearAppLockPin() {
        settingsRepository.setAppLockPinHash(null)
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Add a static salt just to prevent rainbow table attacks on simple PINs
        // In a real app, you'd store a unique salt alongside the hash in Settings
        val saltedPin = "mesh_pin_salt_$pin"
        val hashBytes = digest.digest(saltedPin.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(hashBytes, android.util.Base64.NO_WRAP)
    }
}
