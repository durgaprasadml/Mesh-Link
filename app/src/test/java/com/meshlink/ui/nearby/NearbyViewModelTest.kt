package com.meshlink.ui.nearby

import app.cash.turbine.test
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MainDispatcherRule
import com.meshlink.wifi.data.WifiDirectManager
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
class NearbyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var meshRepository: MeshRepository
    private lateinit var userRepository: UserRepository
    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var viewModel: NearbyViewModel

    private val scannedDevicesFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())

    @Before
    fun setup() {
        meshRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        wifiDirectManager = mockk(relaxed = true)

        coEvery { meshRepository.scannedDevices } returns scannedDevicesFlow

        viewModel = NearbyViewModel(meshRepository, userRepository, wifiDirectManager)
    }

    @Test
    fun `uiState emits sorted devices by rssi descending`() = runTest {
        viewModel.uiState.test {
            assertEquals(emptyList<BleDevice>(), awaitItem().devices)

            val device1 = BleDevice("mesh1", "Device 1", "addr1", -80, System.currentTimeMillis())
            val device2 = BleDevice("mesh2", "Device 2", "addr2", -60, System.currentTimeMillis())
            val device3 = BleDevice("mesh3", "Device 3", "addr3", -70, System.currentTimeMillis())

            scannedDevicesFlow.value = mapOf(
                "addr1" to device1,
                "addr2" to device2,
                "addr3" to device3
            )

            val state = awaitItem()
            assertEquals(3, state.devices.size)
            // Sorted descending: -60, -70, -80
            assertEquals(device2, state.devices[0])
            assertEquals(device3, state.devices[1])
            assertEquals(device1, state.devices[2])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startDiscovery calls meshRepository and wifiDirectManager when user is not null`() = runTest {
        val user = UserEntity("mesh1", "Alice", "123", "hash")
        coEvery { userRepository.getLocalUser() } returns user

        viewModel.startDiscovery()
        advanceUntilIdle()

        coVerify(exactly = 1) { meshRepository.autoStartMesh() }
        coVerify(exactly = 1) { wifiDirectManager.startDiscovery() }
    }

    @Test
    fun `startDiscovery does nothing when user is null`() = runTest {
        coEvery { userRepository.getLocalUser() } returns null

        viewModel.startDiscovery()
        advanceUntilIdle()

        coVerify(exactly = 0) { meshRepository.autoStartMesh() }
        coVerify(exactly = 0) { wifiDirectManager.startDiscovery() }
    }
}
