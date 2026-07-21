package com.meshlink.ui.profile

import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: ProfileViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUserProfile updates uiState with user`() = runTest {
        val testUser = User("mesh_123", "Alice", "+123456789", null, null)
        coEvery { userRepository.getLocalUser() } returns testUser

        viewModel = ProfileViewModel(userRepository)
        
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(testUser, state.user)
    }

    @Test
    fun `saveProfile updates repository and refreshes user`() = runTest {
        val updatedUser = User("mesh_123", "Bob", "+123456789", "content://avatar", "I am Bob")
        coEvery { userRepository.getLocalUser() } returns updatedUser

        viewModel = ProfileViewModel(userRepository)
        advanceUntilIdle()

        viewModel.saveProfile("Bob", "I am Bob", "content://avatar")
        advanceUntilIdle()

        coVerify { userRepository.updateProfile("Bob", "I am Bob", "content://avatar") }

        val state = viewModel.uiState.first()
        assertFalse(state.isSaving)
        assertEquals(updatedUser, state.user)
        assertNull(state.saveError)
    }

    @Test
    fun `saveProfile handles errors gracefully`() = runTest {
        coEvery { userRepository.updateProfile(any(), any(), any()) } throws Exception("Database error")

        viewModel = ProfileViewModel(userRepository)
        advanceUntilIdle()

        viewModel.saveProfile("Bob", "I am Bob", "content://avatar")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isSaving)
        assertEquals("Database error", state.saveError)
    }

    @Test
    fun `dismissError clears saveError`() = runTest {
        coEvery { userRepository.updateProfile(any(), any(), any()) } throws Exception("Database error")

        viewModel = ProfileViewModel(userRepository)
        advanceUntilIdle()

        viewModel.saveProfile("Bob", "I am Bob", "content://avatar")
        advanceUntilIdle()

        viewModel.dismissError()
        
        val state = viewModel.uiState.first()
        assertNull(state.saveError)
    }
}
