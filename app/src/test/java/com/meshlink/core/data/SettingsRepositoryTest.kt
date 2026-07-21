package com.meshlink.core.data

import com.meshlink.core.data.source.SettingsLocalDataSource
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var localDataSource: SettingsLocalDataSource
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        localDataSource = mockk(relaxed = true)
        
        // Mock default flows
        every { localDataSource.isAppLockEnabled } returns flowOf(false)
        every { localDataSource.isBleEnabled } returns flowOf(true)
        every { localDataSource.isWifiDirectEnabled } returns flowOf(true)
        every { localDataSource.preferredTransport } returns flowOf("HYBRID")
        every { localDataSource.isMeshRelayEnabled } returns flowOf(true)

        settingsRepository = SettingsRepositoryImpl(localDataSource)
    }

    @Test
    fun `isAppLockEnabled returns flow from local data source`() = runTest {
        settingsRepository.isAppLockEnabled.collect {
            assertEquals(false, it)
        }
    }

    @Test
    fun `setAppLockEnabled calls local data source`() = runTest {
        settingsRepository.setAppLockEnabled(true)
        coVerify { localDataSource.setAppLockEnabled(true) }
    }

    @Test
    fun `isBleEnabled returns flow from local data source`() = runTest {
        settingsRepository.isBleEnabled.collect {
            assertEquals(true, it)
        }
    }

    @Test
    fun `setPreferredTransport calls local data source`() = runTest {
        settingsRepository.setPreferredTransport("WIFI")
        coVerify { localDataSource.setPreferredTransport("WIFI") }
    }

    @Test
    fun `setMeshRelayEnabled calls local data source`() = runTest {
        settingsRepository.setMeshRelayEnabled(false)
        coVerify { localDataSource.setMeshRelayEnabled(false) }
    }
}
