package com.meshlink.transport

import com.meshlink.ble.api.BleTransport
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.wifi.api.WifiTransport
import com.meshlink.wifi.data.WifiDirectManager
import com.meshlink.wifi.data.WifiP2pConnectionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HybridTransportManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)

    private val bleTransport: BleTransport = mockk(relaxed = true)
    private val wifiTransport: WifiTransport = mockk(relaxed = true)
    private val wifiDirectManager: WifiDirectManager = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private lateinit var hybridTransportManager: HybridTransportManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { bleTransport.incomingPackets } returns MutableSharedFlow()
        every { wifiTransport.incomingPackets } returns MutableSharedFlow()
        every { bleTransport.connectedPeers } returns setOf("peer_ble_1")
        every { wifiTransport.connectedPeers } returns setOf("peer_wifi_1")

        every { wifiDirectManager.connectionState } returns MutableStateFlow(WifiP2pConnectionState.CONNECTED)
        every { wifiDirectManager.isP2pEnabled } returns MutableStateFlow(true)
        every { settingsRepository.preferredTransport } returns flowOf("AUTOMATIC")

        hybridTransportManager = HybridTransportManager(
            bleTransport = bleTransport,
            wifiTransport = wifiTransport,
            wifiDirectManager = wifiDirectManager,
            settingsRepository = settingsRepository,
            applicationScope = scope
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTextPacketPrefersBle() {
        val route = hybridTransportManager.getSelectedRouteType(
            targetId = "peer_1",
            packetType = PacketType.TEXT,
            payloadSize = 100L
        )
        assertEquals(RouteType.BLE, route)
    }

    @Test
    fun testMediaPacketPrefersWifiDirectWhenConnected() {
        every { wifiTransport.isConnected } returns true
        every { wifiTransport.connectedPeers } returns setOf("peer_1")

        val route = hybridTransportManager.getSelectedRouteType(
            targetId = "peer_1",
            packetType = PacketType.MEDIA_CHUNK,
            payloadSize = 50000L
        )
        assertEquals(RouteType.WIFI_DIRECT, route)
    }

    @Test
    fun testSosPacketReturnsHybrid() {
        val route = hybridTransportManager.getSelectedRouteType(
            targetId = "BROADCAST",
            packetType = PacketType.SOS,
            payloadSize = 50L
        )
        assertEquals(RouteType.HYBRID, route)
    }

    @Test
    fun testAutomaticDowngradeFallbackToBleOnWifiFailure() = runTest {
        every { wifiTransport.isConnected } returns true
        every { wifiTransport.connectedPeers } returns setOf("target_peer")

        val packet = MeshPacket(
            packetId = "p_1",
            senderId = "my_id",
            targetId = "target_peer",
            payload = "A".repeat(2000),
            type = PacketType.MEDIA_CHUNK
        )

        // Wi-Fi send fails, BLE succeeds
        coEvery { wifiTransport.sendPacket(any()) } returns MeshResult.Error(com.meshlink.domain.model.MeshError.TransportError("Socket dropped"))
        coEvery { bleTransport.sendPacket(any()) } returns MeshResult.Success(Unit)

        val result = hybridTransportManager.sendPacket(packet)

        // Verify result succeeds via BLE fallback
        assertTrue(result is MeshResult.Success)
        coVerify { wifiTransport.sendPacket(packet) }
        coVerify { bleTransport.sendPacket(packet) }

        val metrics = hybridTransportManager.metrics.value
        assertEquals(1L, metrics.fallbackCount)
        assertEquals(1L, metrics.retryCount)
    }
}
