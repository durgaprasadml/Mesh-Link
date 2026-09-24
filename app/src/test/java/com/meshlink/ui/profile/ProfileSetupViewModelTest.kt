package com.meshlink.ui.profile

import android.net.Uri
import com.meshlink.domain.model.MeshError
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.profile.ProfilePhotoManager
import com.meshlink.profile.ProfileSyncManager
import com.meshlink.service.MeshLifecycleManager
import com.meshlink.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val profilePhotoManager = mockk<ProfilePhotoManager>(relaxed = true)
    private val profileSyncManager = mockk<ProfileSyncManager>(relaxed = true)
    private val meshLifecycleManager = mockk<MeshLifecycleManager>(relaxed = true)

    private lateinit var viewModel: ProfileSetupViewModel

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { userRepository.hasProfile } returns flowOf(false)
        viewModel = ProfileSetupViewModel(
            userRepository = userRepository,
            profilePhotoManager = profilePhotoManager,
            profileSyncManager = profileSyncManager,
            meshLifecycleManager = meshLifecycleManager
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        clearAllMocks()
    }

    @Test
    fun `initial state is Idle and selectedAvatarUri is null`() {
        assertEquals(ProfileSetupUiState.Idle, viewModel.uiState.value)
        assertNull(viewModel.selectedAvatarUri.value)
    }

    @Test
    fun `setAvatarUri updates selectedAvatarUri`() {
        viewModel.setAvatarUri("avatar_01")
        assertEquals("avatar_01", viewModel.selectedAvatarUri.value)

        viewModel.setAvatarUri(null)
        assertNull(viewModel.selectedAvatarUri.value)
    }

    @Test
    fun `createProfile with empty or under 2 characters emits Error event`() = runTest(testDispatcher) {
        val events = mutableListOf<ProfileSetupEvent>()
        val job = launch { viewModel.uiEvent.toList(events) }

        viewModel.createProfile("   ")
        testScheduler.advanceUntilIdle()

        viewModel.createProfile("A")
        testScheduler.advanceUntilIdle()

        assertEquals(2, events.size)
        assertTrue(events[0] is ProfileSetupEvent.Error)
        assertEquals("Display name must be between 2 and 30 characters", (events[0] as ProfileSetupEvent.Error).message)
        assertTrue(events[1] is ProfileSetupEvent.Error)

        coVerify(exactly = 0) { userRepository.setupProfile(any(), any()) }
        assertEquals(ProfileSetupUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `createProfile with name longer than 30 characters emits Error event`() = runTest(testDispatcher) {
        val events = mutableListOf<ProfileSetupEvent>()
        val job = launch { viewModel.uiEvent.toList(events) }

        val longName = "A".repeat(31)
        viewModel.createProfile(longName)
        testScheduler.advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is ProfileSetupEvent.Error)
        assertEquals("Display name must be between 2 and 30 characters", (events[0] as ProfileSetupEvent.Error).message)

        coVerify(exactly = 0) { userRepository.setupProfile(any(), any()) }
        job.cancel()
    }

    @Test
    fun `createProfile with valid name without avatar succeeds and initializes mesh`() = runTest(testDispatcher) {
        val events = mutableListOf<ProfileSetupEvent>()
        val job = launch { viewModel.uiEvent.toList(events) }

        coEvery { userRepository.setupProfile("Alice", null) } returns MeshResult.Success(Unit)

        viewModel.createProfile("  Alice  ")
        testScheduler.advanceUntilIdle()

        coVerify { userRepository.setupProfile("Alice", null) }
        coVerify { meshLifecycleManager.initializeAfterOnboarding() }

        assertEquals(1, events.size)
        assertTrue(events[0] is ProfileSetupEvent.SetupSuccess)
        assertEquals(ProfileSetupUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `createProfile with valid name and content avatar processes photo`() = runTest(testDispatcher) {
        val events = mutableListOf<ProfileSetupEvent>()
        val job = launch { viewModel.uiEvent.toList(events) }

        val mockUri = mockk<Uri>()
        every { mockUri.scheme } returns "content"
        every { Uri.parse("content://media/external/images/1") } returns mockUri

        val localUser = User(meshId = "mesh_test_id", name = "Alice")
        coEvery { userRepository.setupProfile("Alice", "content://media/external/images/1") } returns MeshResult.Success(Unit)
        coEvery { userRepository.getLocalUser() } returns localUser

        val tempFile = File.createTempFile("photo_test", ".jpg")
        tempFile.deleteOnExit()
        coEvery { profilePhotoManager.processAndSavePhoto("mesh_test_id", mockUri) } returns Pair(tempFile, "hash123")

        viewModel.setAvatarUri("content://media/external/images/1")
        viewModel.createProfile("Alice")
        testScheduler.advanceUntilIdle()

        coVerify { userRepository.setupProfile("Alice", "content://media/external/images/1") }
        coVerify { profilePhotoManager.processAndSavePhoto("mesh_test_id", mockUri) }
        coVerify { userRepository.updateProfilePhoto(
            meshId = "mesh_test_id",
            photoPath = tempFile.absolutePath,
            photoHash = "hash123",
            version = any(),
            lastUpdated = any()
        ) }
        coVerify { profileSyncManager.notifyProfilePhotoUpdated(tempFile, "hash123") }
        coVerify { meshLifecycleManager.initializeAfterOnboarding() }

        assertEquals(1, events.size)
        assertTrue(events[0] is ProfileSetupEvent.SetupSuccess)
        assertEquals(ProfileSetupUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `createProfile failure emits error event and resets uiState to Idle`() = runTest(testDispatcher) {
        val events = mutableListOf<ProfileSetupEvent>()
        val job = launch { viewModel.uiEvent.toList(events) }

        coEvery { userRepository.setupProfile("Bob", null) } returns MeshResult.Error(
            MeshError.UnknownError("Database disk full", null)
        )

        viewModel.createProfile("Bob")
        testScheduler.advanceUntilIdle()

        coVerify { userRepository.setupProfile("Bob", null) }
        coVerify(exactly = 0) { meshLifecycleManager.initializeAfterOnboarding() }

        assertEquals(1, events.size)
        assertTrue(events[0] is ProfileSetupEvent.Error)
        assertEquals("Database disk full", (events[0] as ProfileSetupEvent.Error).message)
        assertEquals(ProfileSetupUiState.Idle, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `resetState resets uiState to Idle`() {
        viewModel.resetState()
        assertEquals(ProfileSetupUiState.Idle, viewModel.uiState.value)
    }
}
