package com.meshlink.ui.profile

import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val userRepository = mockk<UserRepository>(relaxed = true)

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init loads user profile into uiState`() = runTest(testDispatcher) {
        val user = User(meshId = "u_100", name = "Alice", aboutMe = "Developer")
        coEvery { userRepository.getLocalUser() } returns user

        viewModel = ProfileViewModel(userRepository)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(user, viewModel.uiState.value.user)
    }

    @Test
    fun `saveProfile updates profile and refreshes local user`() = runTest(testDispatcher) {
        val initialUser = User(meshId = "u_100", name = "Alice")
        val updatedUser = User(meshId = "u_100", name = "Alice Wonder", aboutMe = "Architect")

        coEvery { userRepository.getLocalUser() } returnsMany listOf(initialUser, updatedUser)

        viewModel = ProfileViewModel(userRepository)
        testScheduler.advanceUntilIdle()

        viewModel.saveProfile("Alice Wonder", "Architect", null)
        testScheduler.advanceUntilIdle()

        coVerify { userRepository.updateProfile("Alice Wonder", "Architect", null) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(updatedUser, viewModel.uiState.value.user)
        assertNull(viewModel.uiState.value.saveError)
    }

    @Test
    fun `saveProfile handles failure and emits saveError`() = runTest(testDispatcher) {
        val initialUser = User(meshId = "u_100", name = "Alice")
        coEvery { userRepository.getLocalUser() } returns initialUser
        coEvery { userRepository.updateProfile(any(), any(), any()) } throws RuntimeException("Database write failed")

        viewModel = ProfileViewModel(userRepository)
        testScheduler.advanceUntilIdle()

        viewModel.saveProfile("Alice", "Bio", null)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Database write failed", viewModel.uiState.value.saveError)
    }

    @Test
    fun `dismissError clears saveError state`() = runTest(testDispatcher) {
        coEvery { userRepository.updateProfile(any(), any(), any()) } throws RuntimeException("Error")
        viewModel = ProfileViewModel(userRepository)
        testScheduler.advanceUntilIdle()

        viewModel.saveProfile("Bob", null, null)
        testScheduler.advanceUntilIdle()

        assertEquals("Error", viewModel.uiState.value.saveError)

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.saveError)
    }
}
