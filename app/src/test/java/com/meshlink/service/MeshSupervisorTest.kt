package com.meshlink.service

import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.RoutingTable
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.wifi.manager.WifiDirectManager
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshSupervisorTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockBleScannerManager = mockk<BleScannerManager>(relaxed = true)
    private val mockBleAdvertiserManager = mockk<BleAdvertiserManager>(relaxed = true)
    private val mockWifiDirectManager = mockk<WifiDirectManager>(relaxed = true)
    private val mockDiscoveryEngine = mockk<DiscoveryEngine>(relaxed = true)
    private val mockRouter = mockk<Router>(relaxed = true)
    private val mockRoutingTable = mockk<RoutingTable>(relaxed = true)
    private val mockMeshRepository = mockk<MeshRepository>(relaxed = true)
    private val mockTransportDiagnostics = mockk<TransportDiagnostics>(relaxed = true)

    private val wifiRadioStateFlow = MutableStateFlow(RadioState.STOPPED)

    @Before
    fun setup() {
        every { mockWifiDirectManager.radioState } returns wifiRadioStateFlow
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun testRadioSubsystemValues() {
        val subsystems = RadioSubsystem.values()
        assertEquals(7, subsystems.size)
    }

    @Test
    fun testRadioStateTransitions() {
        val states = RadioState.values()
        assertNotNull(states)
        assertEquals(5, states.size)
        assertEquals(RadioState.RUNNING, RadioState.valueOf("RUNNING"))
    }

    @Test
    fun `isFullyOperational returns true when BLE subsystems are RUNNING even if WIFI_DIRECT is STOPPED`() {
        val supervisor = MeshSupervisor(
            mockBleScannerManager,
            mockBleAdvertiserManager,
            mockWifiDirectManager,
            mockDiscoveryEngine,
            mockRouter,
            mockRoutingTable,
            mockMeshRepository,
            mockTransportDiagnostics,
            testScope
        )

        supervisor.startAllSubsystems()

        supervisor.updateSubsystemState(RadioSubsystem.BLE_SCANNER, RadioState.RUNNING)
        supervisor.updateSubsystemState(RadioSubsystem.BLE_ADVERTISER, RadioState.RUNNING)
        supervisor.updateSubsystemState(RadioSubsystem.GATT_SERVER, RadioState.RUNNING)
        supervisor.updateSubsystemState(RadioSubsystem.WIFI_DIRECT, RadioState.STOPPED)
        supervisor.updateSubsystemState(RadioSubsystem.DISCOVERY_ENGINE, RadioState.RUNNING)
        supervisor.updateSubsystemState(RadioSubsystem.ROUTING_ENGINE, RadioState.RUNNING)
        supervisor.updateSubsystemState(RadioSubsystem.PACKET_DISPATCHER, RadioState.RUNNING)

        // WIFI_DIRECT is STOPPED, BLE subsystems are RUNNING
        assertTrue(supervisor.isFullyOperational())
        assertEquals(RadioState.STOPPED, supervisor.subsystemStates.value[RadioSubsystem.WIFI_DIRECT])
    }
}
