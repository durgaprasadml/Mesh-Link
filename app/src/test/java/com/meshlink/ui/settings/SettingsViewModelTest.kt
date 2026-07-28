package com.meshlink.ui.settings

import com.meshlink.domain.model.User
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import com.meshlink.util.MainDispatcherRule
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        every { userRepository.isEncryptionEnabled } returns flowOf(true)
        every { userRepository.isOnlineVisible } returns flowOf(true)
        every { userRepository.meshMode } returns flowOf("Auto")

        every { settingsRepository.isBleEnabled } returns flowOf(true)
        every { settingsRepository.bleAdvertisingEnabled } returns flowOf(true)
        every { settingsRepository.bleScanningEnabled } returns flowOf(true)
        every { settingsRepository.bleTxPower } returns flowOf(2)
        every { settingsRepository.bleScanInterval } returns flowOf(5000L)
        every { settingsRepository.bleAutoRestart } returns flowOf(true)
        every { settingsRepository.preferredTransport } returns flowOf("BLE")
        every { settingsRepository.isMeshRelayEnabled } returns flowOf(true)
        every { settingsRepository.meshMaxHops } returns flowOf(5)
        every { settingsRepository.meshTtl } returns flowOf(10)
        every { settingsRepository.meshPriority } returns flowOf(1)
        every { settingsRepository.meshQueueSize } returns flowOf(1000)
        every { settingsRepository.advancedEncryptionEnforcement } returns flowOf(true)
        every { settingsRepository.themeMode } returns flowOf("SYSTEM")
        every { settingsRepository.isMaterialYouEnabled } returns flowOf(true)
        every { settingsRepository.fontScale } returns flowOf(1.0f)
        every { settingsRepository.highContrast } returns flowOf(false)
        every { settingsRepository.accentColor } returns flowOf("Blue")
        every { settingsRepository.animationsEnabled } returns flowOf(true)
        every { settingsRepository.glassEffectsEnabled } returns flowOf(true)
        every { settingsRepository.cornerRadiusScale } returns flowOf(1.0f)
        every { settingsRepository.largeTextEnabled } returns flowOf(false)
        every { settingsRepository.reduceMotionEnabled } returns flowOf(false)

        viewModel = SettingsViewModel(userRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `updateUserName calls repository and updates local user`() = runTest(testDispatcher) {
        val updatedUser = User(meshId = "u1", name = "New Name")
        coEvery { userRepository.getLocalUser() } returns updatedUser

        viewModel.updateUserName("New Name")
        testScheduler.advanceUntilIdle()

        coVerify { userRepository.updateUserName("New Name") }
    }

    @Test
    fun `setMeshRelayEnabled calls settings repository`() = runTest(testDispatcher) {
        viewModel.setMeshRelayEnabled(false)
        testScheduler.advanceUntilIdle()

        coVerify { settingsRepository.setMeshRelayEnabled(false) }
    }

    @Test
    fun `setMeshMaxHops calls settings repository`() = runTest(testDispatcher) {
        viewModel.setMeshMaxHops(8)
        testScheduler.advanceUntilIdle()

        coVerify { settingsRepository.setMeshMaxHops(8) }
    }

    @Test
    fun `setAdvancedEncryptionEnforcement updates setting`() = runTest(testDispatcher) {
        viewModel.setAdvancedEncryptionEnforcement(false)
        testScheduler.advanceUntilIdle()

        coVerify { settingsRepository.setAdvancedEncryptionEnforcement(false) }
    }
}
