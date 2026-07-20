package com.meshlink.ui.settings

import com.meshlink.domain.model.User
import com.meshlink.domain.repository.SecurityRepository
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SecurityLogManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import app.cash.turbine.test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var sessionManager: SessionManager
    private lateinit var trustManager: TrustManager
    private lateinit var securityRepository: SecurityRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var userRepository: UserRepository
    private lateinit var logManager: SecurityLogManager
    private lateinit var viewModel: SecurityViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        cryptoManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        trustManager = mockk(relaxed = true)
        securityRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        logManager = mockk(relaxed = true)

        coEvery { userRepository.getLocalUser() } returns User("123", "Alice", "123456789")
        coEvery { settingsRepository.isAppLockEnabled } returns flowOf(true)
        coEvery { settingsRepository.autoLockTimeoutMs } returns flowOf(60000L)
        coEvery { settingsRepository.isBiometricsEnabled } returns flowOf(false)
        coEvery { settingsRepository.appLockPinHash } returns flowOf("hashed_pin")
        every { trustManager.trustStates } returns kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
        every { logManager.logsFlow } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())

        every { cryptoManager.getLocalFingerprint() } returns "fingerprint"
        every { cryptoManager.getOrCreatePublicKey() } returns "pubkey"
        every { cryptoManager.getKeyCreationTime() } returns 1000L
        every { cryptoManager.getLastRotationTime() } returns 1000L
        every { sessionManager.getAllSessionPeers() } returns setOf("peer1")

        viewModel = SecurityViewModel(
            cryptoManager, sessionManager, trustManager, securityRepository,
            settingsRepository, userRepository, logManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest {
        viewModel.uiState.test {
            // Initial state from StateFlow (may be the default before combine emits)
            val initialState = awaitItem()
            
            // The first real emission from combine
            val state = awaitItem()
            
            assertEquals("123", state.meshId)
            assertEquals("fingerprint", state.deviceFingerprint)
            assertEquals("pubkey", state.publicKey)
            assertEquals(true, state.isAppLockEnabled)
            assertEquals(true, state.hasPinConfigured)
            assertEquals(setOf("peer1"), state.activeSessions)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testConfigurePin() = runTest {
        viewModel.configurePin("1234")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { securityRepository.setAppLockPin("1234") }
        coVerify { settingsRepository.setAppLockEnabled(true) }
        verify { logManager.log("App Lock", "PIN configured", any()) }
    }

    @Test
    fun testRotateIdentityKeys() = runTest {
        viewModel.rotateIdentityKeys()
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify { cryptoManager.rotateIdentityKeys() }
        verify { logManager.log("Key Management", "Identity keys rotated", any()) }
    }

    @Test
    fun testExportIdentity() = runTest {
        every { cryptoManager.exportIdentity() } returns "base64_identity"
        
        viewModel.uiState.test {
            awaitItem() // Initial
            awaitItem() // Combine
            
            viewModel.exportIdentity()
            
            val state = awaitItem()
            assertEquals("base64_identity", state.exportedIdentityBase64)
            verify { cryptoManager.exportIdentity() }
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testBlockDevice() = runTest {
        viewModel.blockDevice("peer_bad")
        
        verify { trustManager.blockPeer("peer_bad") }
        verify { sessionManager.removeSession("peer_bad") }
    }
}
