package com.meshlink.security.data

import com.meshlink.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

class SecurityRepositoryImplTest {

    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var securityRepository: SecurityRepositoryImpl

    @Before
    fun setup() {
        io.mockk.mockkStatic(android.util.Base64::class)
        io.mockk.every { android.util.Base64.encodeToString(any(), any()) } answers {
            val bytes = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(bytes)
        }

        cryptoManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        securityRepository = SecurityRepositoryImpl(cryptoManager, settingsRepository)
    }

    @Test
    fun testSetAppLockPin() = runTest {
        securityRepository.setAppLockPin("1234")
        
        val expectedHash = hashPin("1234")
        coVerify { settingsRepository.setAppLockPinHash(expectedHash) }
    }

    @Test
    fun testVerifyAppLockPin() = runTest {
        val expectedHash = hashPin("1234")
        coEvery { settingsRepository.appLockPinHash } returns flowOf(expectedHash)
        
        val isCorrect = securityRepository.verifyAppLockPin("1234")
        assertTrue(isCorrect)
        
        val isIncorrect = securityRepository.verifyAppLockPin("4321")
        assertFalse(isIncorrect)
    }

    @Test
    fun testHasAppLockPin() = runTest {
        coEvery { settingsRepository.appLockPinHash } returns flowOf(hashPin("1234"))
        assertTrue(securityRepository.hasAppLockPin())
        
        coEvery { settingsRepository.appLockPinHash } returns flowOf(null)
        assertFalse(securityRepository.hasAppLockPin())
    }

    @Test
    fun testClearAppLockPin() = runTest {
        securityRepository.clearAppLockPin()
        coVerify { settingsRepository.setAppLockPinHash(null) }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPin = "mesh_pin_salt_$pin"
        val hashBytes = digest.digest(saltedPin.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(hashBytes, android.util.Base64.NO_WRAP)
    }
}
