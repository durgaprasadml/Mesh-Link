package com.meshlink.ui.settings

import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        coEvery { userRepository.isEncryptionEnabled } returns flowOf(true)
        coEvery { userRepository.isOnlineVisible } returns flowOf(true)
        coEvery { userRepository.meshMode } returns flowOf("Auto")
        coEvery { settingsRepository.isAppLockEnabled } returns flowOf(false)
        coEvery { settingsRepository.autoLockTimeoutMs } returns flowOf(60000L)
        coEvery { settingsRepository.isBiometricsEnabled } returns flowOf(false)
        coEvery { settingsRepository.isBleEnabled } returns flowOf(true)
        coEvery { settingsRepository.isWifiDirectEnabled } returns flowOf(true)
        coEvery { settingsRepository.preferredTransport } returns flowOf("HYBRID")
        coEvery { settingsRepository.isMeshRelayEnabled } returns flowOf(true)
        coEvery { settingsRepository.themeMode } returns flowOf("SYSTEM")
        coEvery { settingsRepository.isMaterialYouEnabled } returns flowOf(true)
        coEvery { settingsRepository.fontScale } returns flowOf(1.0f)
        coEvery { settingsRepository.highContrast } returns flowOf(false)

        viewModel = SettingsViewModel(userRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateUserName() = runTest {
        val newName = "New Name"
        viewModel.updateUserName(newName)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { userRepository.updateUserName(newName) }
    }

    @Test
    fun testSetThemeMode() = runTest {
        val mode = "DARK"
        viewModel.setThemeMode(mode)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { settingsRepository.setThemeMode(mode) }
    }

    @Test
    fun testSetAppLockEnabled() = runTest {
        viewModel.setAppLockEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { settingsRepository.setAppLockEnabled(true) }
    }

    @Test
    fun testSetBleEnabled() = runTest {
        viewModel.setBleEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { settingsRepository.setBleEnabled(false) }
    }
}
