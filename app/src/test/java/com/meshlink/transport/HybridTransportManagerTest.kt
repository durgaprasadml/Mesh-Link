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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HybridTransportManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)

    private val bleTransport: BleTransport = mock()
    private val wifiTransport: WifiTransport = mock()
    private val wifiDirectManager: WifiDirectManager = mock()
    private val settingsRepository: SettingsRepository = mock()

    private lateinit var hybridTransportManager: HybridTransportManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(bleTransport.incomingPackets).thenReturn(flowOf())
        whenever(wifiTransport.incomingPackets).thenReturn(flowOf())
        whenever(bleTransport.connectedPeers).thenReturn(setOf("peer_ble_1"))
        whenever(wifiTransport.connectedPeers).thenReturn(setOf("peer_wifi_1"))

        whenever(wifiDirectManager.connectionState).thenReturn(MutableStateFlow(WifiP2pConnectionState.CONNECTED))
        whenever(wifiDirectManager.isP2pEnabled).thenReturn(MutableStateFlow(true))
        whenever(settingsRepository.preferredTransport).thenReturn(flowOf("AUTOMATIC"))

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
        whenever(wifiTransport.isConnected).thenReturn(true)
        whenever(wifiTransport.connectedPeers).thenReturn(setOf("peer_1"))

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
        whenever(wifiTransport.isConnected).thenReturn(true)
        whenever(wifiTransport.connectedPeers).thenReturn(setOf("target_peer"))

        val packet = MeshPacket(
            packetId = "p_1",
            senderId = "my_id",
            targetId = "target_peer",
            payload = "A".repeat(2000),
            type = PacketType.MEDIA_CHUNK
        )

        // Wi-Fi send fails, BLE succeeds
        whenever(wifiTransport.sendPacket(any())).thenReturn(MeshResult.Error(com.meshlink.domain.model.MeshError.TransportError("Socket dropped")))
        whenever(bleTransport.sendPacket(any())).thenReturn(MeshResult.Success(Unit))

        val result = hybridTransportManager.sendPacket(packet)

        // Verify result succeeds via BLE fallback
        assertTrue(result is MeshResult.Success)
        verify(wifiTransport).sendPacket(packet)
        verify(bleTransport).sendPacket(packet)

        val metrics = hybridTransportManager.metrics.value
        assertEquals(1L, metrics.fallbackCount)
        assertEquals(1L, metrics.retryCount)
    }
}
