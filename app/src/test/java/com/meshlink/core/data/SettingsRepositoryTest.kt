package com.meshlink.core.data

import com.meshlink.core.data.source.SettingsLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class SettingsRepositoryTest {

    @get:org.junit.Rule
    val mainDispatcherRule = com.meshlink.util.MainDispatcherRule()

    private lateinit var localDataSource: SettingsLocalDataSource
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        localDataSource = mockk(relaxed = true)
    }

    @Test
    fun testIsAppLockEnabledFlowsFromDataSource() = runTest {
        coEvery { localDataSource.isAppLockEnabled } returns flowOf(true)
        settingsRepository = SettingsRepositoryImpl(localDataSource)
        var result = false
        settingsRepository.isAppLockEnabled.collect { result = it }
        assertEquals(true, result)
    }

    @Test
    fun testSetAppLockEnabledDelegatesToDataSource() = runTest {
        settingsRepository = SettingsRepositoryImpl(localDataSource)
        settingsRepository.setAppLockEnabled(true)
        coVerify { localDataSource.setAppLockEnabled(true) }
    }

    @Test
    fun testSetThemeModeDelegatesToDataSource() = runTest {
        settingsRepository = SettingsRepositoryImpl(localDataSource)
        settingsRepository.setThemeMode("DARK")
        coVerify { localDataSource.setThemeMode("DARK") }
    }
}
