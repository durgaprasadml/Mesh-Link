package com.meshlink.ui.settings

import com.meshlink.domain.model.User
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userRepository: UserRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        userRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        val fakeUser = User(meshId = "mesh_1", name = "Test User", phoneNumber = "1234567890")
        coEvery { userRepository.getLocalUser() } returns fakeUser
        every { userRepository.isEncryptionEnabled } returns flowOf(true)
        every { userRepository.isOnlineVisible } returns flowOf(true)
        every { userRepository.meshMode } returns flowOf("Auto")

        // Security
        every { settingsRepository.isAppLockEnabled } returns flowOf(false)
        every { settingsRepository.autoLockTimeoutMs } returns flowOf(60000L)
        every { settingsRepository.isBiometricsEnabled } returns flowOf(false)

        // Network - Bluetooth
        every { settingsRepository.isBleEnabled } returns flowOf(true)
        every { settingsRepository.bleAdvertisingEnabled } returns flowOf(true)
        every { settingsRepository.bleScanningEnabled } returns flowOf(true)
        every { settingsRepository.bleTxPower } returns flowOf(2)
        every { settingsRepository.bleScanInterval } returns flowOf(5000L)
        every { settingsRepository.bleAutoRestart } returns flowOf(true)

        // Network - WiFi
        every { settingsRepository.isWifiDirectEnabled } returns flowOf(true)
        every { settingsRepository.wifiAutoConnect } returns flowOf(true)
        every { settingsRepository.wifiPeerDiscoveryEnabled } returns flowOf(true)
        every { settingsRepository.wifiPreferredGroupOwner } returns flowOf(false)
        every { settingsRepository.wifiReconnectEnabled } returns flowOf(true)

        // Transport
        every { settingsRepository.preferredTransport } returns flowOf("HYBRID")

        // Relay
        every { settingsRepository.isMeshRelayEnabled } returns flowOf(true)
        every { settingsRepository.meshMaxHops } returns flowOf(5)
        every { settingsRepository.meshTtl } returns flowOf(10)
        every { settingsRepository.meshPriority } returns flowOf(1)
        every { settingsRepository.meshQueueSize } returns flowOf(1000)

        // Discovery
        every { settingsRepository.discoveryInterval } returns flowOf(30000L)
        every { settingsRepository.discoveryBackground } returns flowOf(true)
        every { settingsRepository.discoveryForeground } returns flowOf(true)
        every { settingsRepository.discoveryTimeout } returns flowOf(120000L)
        every { settingsRepository.discoveryRestart } returns flowOf(true)

        // Advanced
        every { settingsRepository.advancedPacketSize } returns flowOf(512)
        every { settingsRepository.advancedRetryCount } returns flowOf(3)
        every { settingsRepository.advancedCompression } returns flowOf(true)
        every { settingsRepository.advancedEncryptionEnforcement } returns flowOf(true)
        every { settingsRepository.advancedBandwidthOptimization } returns flowOf(true)

        // Appearance
        every { settingsRepository.themeMode } returns flowOf("SYSTEM")
        every { settingsRepository.isMaterialYouEnabled } returns flowOf(true)
        every { settingsRepository.fontScale } returns flowOf(1.0f)
        every { settingsRepository.highContrast } returns flowOf(false)

        viewModel = SettingsViewModel(userRepository, settingsRepository)
    }

    @Test
    fun `initial uiState is populated from repositories`() = runTest {
        val collectJob = backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Test User", state.user?.name)
        assertEquals(true, state.isEncryptionEnabled)
        assertEquals(false, state.isAppLockEnabled)
        assertEquals(true, state.isBleEnabled)
        assertEquals("HYBRID", state.preferredTransport)
        assertEquals(true, state.isMeshRelayEnabled)
        assertEquals(5, state.meshMaxHops)
    }

    @Test
    fun `setPreferredTransport updates repository`() = runTest {
        viewModel.setPreferredTransport("BLE")
        coVerify { settingsRepository.setPreferredTransport("BLE") }
    }

    @Test
    fun `setMeshRelayEnabled updates repository`() = runTest {
        viewModel.setMeshRelayEnabled(false)
        coVerify { settingsRepository.setMeshRelayEnabled(false) }
    }

    @Test
    fun `setBleEnabled updates repository`() = runTest {
        viewModel.setBleEnabled(false)
        coVerify { settingsRepository.setBleEnabled(false) }
    }
}
