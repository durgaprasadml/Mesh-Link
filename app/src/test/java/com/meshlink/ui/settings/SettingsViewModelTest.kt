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
        every { settingsRepository.emergencyNumber } returns flowOf("112")
        every { settingsRepository.sosAutoBroadcast } returns flowOf(true)
        every { settingsRepository.shareLiveLocation } returns flowOf(true)
        every { settingsRepository.emergencyTemplate } returns flowOf("EMERGENCY! Immediate assistance needed at my coordinates.")

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

    @Test
    fun `default SettingsUiState has LIGHT theme mode for first launch`() {
        val defaultState = SettingsUiState()
        assertEquals("LIGHT", defaultState.themeMode)
    }

    @Test
    fun `setThemeMode calls settings repository`() = runTest(testDispatcher) {
        viewModel.setThemeMode("DARK")
        testScheduler.advanceUntilIdle()

        coVerify { settingsRepository.setThemeMode("DARK") }
    }

    @Test
    fun `emergency settings persist to settingsRepository`() = runTest(testDispatcher) {
        viewModel.setEmergencyNumber("108")
        viewModel.setSosAutoBroadcast(false)
        viewModel.setShareLiveLocation(false)
        viewModel.setEmergencyTemplate("Need medical help at Sector 4")
        testScheduler.advanceUntilIdle()

        coVerify { settingsRepository.setEmergencyNumber("108") }
        coVerify { settingsRepository.setSosAutoBroadcast(false) }
        coVerify { settingsRepository.setShareLiveLocation(false) }
        coVerify { settingsRepository.setEmergencyTemplate("Need medical help at Sector 4") }
    }

    @Test
    fun `clearMediaCache and optimizeDatabase execute real maintenance operations`() = runTest(testDispatcher) {
        val cacheManager = mockk<com.meshlink.storage.data.local.CacheManager>(relaxed = true)
        val meshDatabase = mockk<com.meshlink.database.data.local.MeshDatabase>(relaxed = true)
        val openHelper = mockk<androidx.sqlite.db.SupportSQLiteOpenHelper>(relaxed = true)
        val sqliteDb = mockk<androidx.sqlite.db.SupportSQLiteDatabase>(relaxed = true)

        coEvery { cacheManager.clearMediaCache() } returns 2048L
        coEvery { cacheManager.clearOrphanFiles(any()) } returns 1024L
        coEvery { cacheManager.getStorageBreakdown() } returns com.meshlink.storage.data.local.StorageBreakdown(
            databaseBytes = 4096L,
            mediaCacheBytes = 0L,
            voiceNotesBytes = 512L,
            freeSpaceBytes = 100_000L,
            totalSpaceBytes = 200_000L
        )
        every { meshDatabase.openHelper } returns openHelper
        every { openHelper.writableDatabase } returns sqliteDb
        every { openHelper.readableDatabase } returns sqliteDb

        val vmWithStorage = SettingsViewModel(userRepository, settingsRepository, cacheManager, meshDatabase).apply {
            ioDispatcher = testDispatcher
        }
        vmWithStorage.clearMediaCache()
        vmWithStorage.optimizeDatabase()
        testScheduler.advanceUntilIdle()

        coVerify { cacheManager.clearMediaCache() }
        coVerify { cacheManager.clearOrphanFiles(any()) }
        verify { sqliteDb.execSQL("VACUUM") }
    }

    @Test
    fun `resolveSettingsBackDestination returns HOME for all subscreens and null for HOME`() {
        org.junit.Assert.assertNull(resolveSettingsBackDestination(SettingsDestination.HOME))
        SettingsDestination.entries
            .filter { it != SettingsDestination.HOME }
            .forEach { subDestination ->
                assertEquals(SettingsDestination.HOME, resolveSettingsBackDestination(subDestination))
            }
    }
}
