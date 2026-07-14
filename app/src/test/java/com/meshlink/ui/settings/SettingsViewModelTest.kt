package com.meshlink.ui.settings

import app.cash.turbine.test
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: SettingsViewModel

    private val isEncryptionEnabledFlow = MutableStateFlow(true)
    private val isOnlineVisibleFlow = MutableStateFlow(true)
    private val meshModeFlow = MutableStateFlow("Auto")
    private val testUser = UserEntity(
        meshId = "user1",
        name = "Test User",
        phoneNumber = "1234567890",
        pinHash = "hash"
    )

    @Before
    fun setup() {
        userRepository = mockk(relaxed = true)
        coEvery { userRepository.isEncryptionEnabled } returns isEncryptionEnabledFlow
        coEvery { userRepository.isOnlineVisible } returns isOnlineVisibleFlow
        coEvery { userRepository.meshMode } returns meshModeFlow
        coEvery { userRepository.getLocalUser() } returns testUser

        viewModel = SettingsViewModel(userRepository)
    }

    @Test
    fun `uiState emits values from repository`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(true, state.isEncryptionEnabled)
            assertEquals(true, state.isOnlineVisible)
            assertEquals("Auto", state.meshMode)
            assertEquals(testUser, state.user)

            isEncryptionEnabledFlow.value = false
            val state2 = awaitItem()
            assertEquals(false, state2.isEncryptionEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setEncryptionEnabled delegates to repository`() = runTest {
        viewModel.setEncryptionEnabled(false)
        advanceUntilIdle()
        coVerify(exactly = 1) { userRepository.setEncryptionEnabled(false) }
    }

    @Test
    fun `setOnlineVisible delegates to repository`() = runTest {
        viewModel.setOnlineVisible(false)
        advanceUntilIdle()
        coVerify(exactly = 1) { userRepository.setOnlineVisible(false) }
    }

    @Test
    fun `setMeshMode delegates to repository`() = runTest {
        viewModel.setMeshMode("Manual")
        advanceUntilIdle()
        coVerify(exactly = 1) { userRepository.setMeshMode("Manual") }
    }

    @Test
    fun `logout delegates to repository and emits LogoutSuccess event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.logout()
            advanceUntilIdle()
            
            coVerify(exactly = 1) { userRepository.logout() }
            assertEquals(SettingsEvent.LogoutSuccess, awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
