package com.meshlink.routing.engine

import com.meshlink.ble.api.BleTransport
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.transport.TransportHealth
import com.meshlink.wifi.api.WifiTransport
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntelligentTransportManagerTest {

    private val bleTransport = mockk<BleTransport>(relaxed = true)
    private val wifiTransport = mockk<WifiTransport>(relaxed = true)
    private val routeOptimizer = mockk<RouteOptimizer>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val metrics = TransportMetrics()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val bleHealthFlow = MutableStateFlow(TransportHealth.CONNECTED)
    private val wifiHealthFlow = MutableStateFlow(TransportHealth.DISCONNECTED)

    private lateinit var manager: IntelligentTransportManager

    @Before
    fun setUp() {
        every { settingsRepository.preferredTransport } returns flowOf("AUTOMATIC")
        every { bleTransport.health } returns bleHealthFlow
        every { wifiTransport.health } returns wifiHealthFlow
        every { bleTransport.connectedPeers } returns setOf("ble_peer_1")
        every { wifiTransport.connectedPeers } returns emptySet()

        coEvery { bleTransport.broadcastPacket(any(), any(), any()) } returns MeshResult.Success(Unit)
        coEvery { wifiTransport.broadcastPacket(any(), any(), any()) } returns MeshResult.Success(Unit)

        manager = IntelligentTransportManager(
            bleTransport = bleTransport,
            wifiTransport = wifiTransport,
            routeOptimizer = routeOptimizer,
            settingsRepository = settingsRepository,
            metrics = metrics,
            applicationScope = testScope
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `selectTransportForPayload routes TEXT ACKs Location Beacons and Key Exchange to BLE`() {
        val textRoute = manager.selectTransportForPayload("target", PacketType.TEXT, 100L)
        val ackRoute = manager.selectTransportForPayload("target", PacketType.DELIVERY_ACK, 50L)
        val readRoute = manager.selectTransportForPayload("target", PacketType.READ_RECEIPT, 50L)
        val locationRoute = manager.selectTransportForPayload("target", PacketType.LOCATION, 80L)

        assertEquals(RouteType.BLE, textRoute)
        assertEquals(RouteType.BLE, ackRoute)
        assertEquals(RouteType.BLE, readRoute)
        assertEquals(RouteType.BLE, locationRoute)
    }

    @Test
    fun `selectTransportForPayload routes Voice Video Images and Files to Wi-Fi Direct`() {
        val voiceRoute = manager.selectTransportForPayload("target", PacketType.VOICE_FRAME, 200L)
        val videoRoute = manager.selectTransportForPayload("target", PacketType.VIDEO_FRAME, 5000L)
        val imageRoute = manager.selectTransportForPayload("target", PacketType.MEDIA_CHUNK, 1024L, mimeType = "image/jpeg")
        val fileRoute = manager.selectTransportForPayload("target", PacketType.MEDIA_META, 2048L, mimeType = "application/pdf")

        assertEquals(RouteType.WIFI_DIRECT, voiceRoute)
        assertEquals(RouteType.WIFI_DIRECT, videoRoute)
        assertEquals(RouteType.WIFI_DIRECT, imageRoute)
        assertEquals(RouteType.WIFI_DIRECT, fileRoute)
    }

    @Test
    fun `selectTransportForPayload routes payloads larger than 50KB to Wi-Fi Direct`() {
        val largeTextRoute = manager.selectTransportForPayload("target", PacketType.TEXT, 60_000L)
        assertEquals(RouteType.WIFI_DIRECT, largeTextRoute)
    }

    @Test
    fun `sendPacket automatically falls back to BLE when Wi-Fi Direct is unavailable for small payloads`() = runTest {
        val smallMediaPacket = MeshPacket(
            packetId = "pkt_small_media",
            senderId = "me",
            targetId = "peer",
            payload = "Small Image Data Bytes",
            type = PacketType.MEDIA_CHUNK,
            mimeType = "image/png"
        )

        val result = manager.sendPacket(smallMediaPacket)

        assertTrue(result is MeshResult.Success)
        coVerify(exactly = 1) { bleTransport.broadcastPacket(smallMediaPacket, excludeAddress = null, includeAddress = null) }
        coVerify(exactly = 0) { wifiTransport.broadcastPacket(any(), any(), any()) }
        assertEquals(1L, metrics.fallbackCount)
        assertEquals(1L, metrics.blePacketCount)
    }

    @Test
    fun `sendPacket keeps large payload queued when Wi-Fi Direct is unavailable`() = runTest {
        val largeBytes = ByteArray(60_000) { 0x01 }
        val largeMediaPacket = MeshPacket(
            packetId = "pkt_large_media",
            senderId = "me",
            targetId = "peer",
            payload = String(largeBytes, Charsets.ISO_8859_1),
            type = PacketType.MEDIA_CHUNK,
            mimeType = "application/octet-stream"
        )

        val result = manager.sendPacket(largeMediaPacket)

        assertTrue(result is MeshResult.Error)
        coVerify(exactly = 0) { bleTransport.broadcastPacket(any(), any(), any()) }
        coVerify(exactly = 0) { wifiTransport.broadcastPacket(any(), any(), any()) }
    }

    @Test
    fun `sendPacket uses Wi-Fi Direct when connected and updates metrics`() = runTest {
        every { wifiTransport.connectedPeers } returns setOf("wifi_peer_1")
        wifiHealthFlow.value = TransportHealth.CONNECTED

        val mediaPacket = MeshPacket(
            packetId = "pkt_media_wifi",
            senderId = "me",
            targetId = "peer",
            payload = "Image Data Bytes",
            type = PacketType.MEDIA_CHUNK,
            mimeType = "image/png"
        )

        val result = manager.sendPacket(mediaPacket)

        assertTrue(result is MeshResult.Success)
        coVerify(exactly = 1) { wifiTransport.broadcastPacket(mediaPacket, excludeAddress = null, includeAddress = null) }
        assertEquals(1L, metrics.wifiPacketCount)
        assertEquals(0L, metrics.fallbackCount)
    }

    @Test
    fun `sendPacket retries Wi-Fi Direct and falls back to BLE if small payload Wi-Fi fails`() = runTest {
        every { wifiTransport.connectedPeers } returns setOf("wifi_peer_1")
        wifiHealthFlow.value = TransportHealth.CONNECTED

        coEvery { wifiTransport.broadcastPacket(any(), any(), any()) } returns MeshResult.Error(
            com.meshlink.domain.model.MeshError.TransportError("Socket Error")
        )

        val smallMediaPacket = MeshPacket(
            packetId = "pkt_media_fail",
            senderId = "me",
            targetId = "peer",
            payload = "Image Data Bytes",
            type = PacketType.MEDIA_CHUNK,
            mimeType = "image/png"
        )

        val result = manager.sendPacket(smallMediaPacket)

        assertTrue(result is MeshResult.Success)
        coVerify(exactly = 2) { wifiTransport.broadcastPacket(smallMediaPacket, excludeAddress = null, includeAddress = null) }
        coVerify(exactly = 1) { bleTransport.broadcastPacket(smallMediaPacket, excludeAddress = null, includeAddress = null) }
        assertEquals(1L, metrics.fallbackCount)
        assertEquals(1L, metrics.retryCount)
    }
}
