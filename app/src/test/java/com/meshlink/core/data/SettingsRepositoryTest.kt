package com.meshlink.core.data

import com.meshlink.core.data.source.SettingsLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {

    private lateinit var localDataSource: SettingsLocalDataSource
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        localDataSource = mockk(relaxed = true)
        settingsRepository = SettingsRepositoryImpl(localDataSource)
    }

    @Test
    fun testIsAppLockEnabledFlowsFromDataSource() = runTest {
        coEvery { localDataSource.isAppLockEnabled } returns flowOf(true)
        var result = false
        settingsRepository.isAppLockEnabled.collect { result = it }
        assertEquals(true, result)
    }

    @Test
    fun testSetAppLockEnabledDelegatesToDataSource() = runTest {
        settingsRepository.setAppLockEnabled(true)
        coVerify { localDataSource.setAppLockEnabled(true) }
    }

    @Test
    fun testSetThemeModeDelegatesToDataSource() = runTest {
        settingsRepository.setThemeMode("DARK")
        coVerify { localDataSource.setThemeMode("DARK") }
    }
}
