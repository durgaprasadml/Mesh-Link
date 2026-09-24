package com.meshlink.ble.data

import com.meshlink.ble.api.BleTransport
import com.meshlink.ble.data.gatt.*
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.ble.discovery.PeerDiscoveryRecord
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.*
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import com.meshlink.util.MeshIdNormalizer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MultiPeerSosMediaTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val cryptoManager = mockk<MeshCryptoManager>(relaxed = true)
    private val trustManager = mockk<TrustManager>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val rekeyManager = mockk<RekeyManager>(relaxed = true)
    private val meshRouter = mockk<Router>(relaxed = true)
    private val connectionManager = mockk<BleConnectionManager>(relaxed = true)
    private val discoveryManager = mockk<DiscoveryManager>(relaxed = true)
    private val discoveryEngine = mockk<DiscoveryEngine>(relaxed = true)
    private val gattManager = mockk<BleGattManager>(relaxed = true)

    private val scannedDevicesFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())

    private lateinit var routingCoordinator: RoutingCoordinator
    private lateinit var bleTransport: BleTransportImpl

    private val peerBMac = "AA:BB:CC:DD:EE:01"
    private val peerBMeshId = "PEERB001"
    private val peerCMac = "AA:BB:CC:DD:EE:02"
    private val peerCMeshId = "PEERC002"

    @Before
    fun setup() {
        every { discoveryManager.scannedDevices } returns scannedDevicesFlow
        every { discoveryManager.discoveryEngine } returns discoveryEngine

        val scannedMap = mapOf(
            peerBMac to BleDevice(name = "Peer B", address = peerBMac, meshId = peerBMeshId, rssi = -60),
            peerCMac to BleDevice(name = "Peer C", address = peerCMac, meshId = peerCMeshId, rssi = -65)
        )
        scannedDevicesFlow.value = scannedMap

        every { discoveryEngine.cache.get(peerBMac) } returns PeerDiscoveryRecord(macAddress = peerBMac, meshId = peerBMeshId, name = "Peer B")
        every { discoveryEngine.cache.get(peerCMac) } returns PeerDiscoveryRecord(macAddress = peerCMac, meshId = peerCMeshId, name = "Peer C")
        every { discoveryEngine.cache.getAll() } returns listOf(
            PeerDiscoveryRecord(macAddress = peerBMac, meshId = peerBMeshId, name = "Peer B"),
            PeerDiscoveryRecord(macAddress = peerCMac, meshId = peerCMeshId, name = "Peer C")
        )

        routingCoordinator = RoutingCoordinator(
            userRepository = userRepository,
            cryptoManager = cryptoManager,
            trustManager = trustManager,
            sessionManager = sessionManager,
            rekeyManager = rekeyManager,
            meshRouter = meshRouter,
            connectionManager = connectionManager,
            discoveryManager = discoveryManager
        )

        bleTransport = BleTransportImpl(
            gattManager = gattManager,
            connectionManager = connectionManager,
            routingCoordinatorProvider = { routingCoordinator },
            applicationScope = testScope
        )
    }

    @Test
    fun `RoutingCoordinator resolves meshId to MAC address and MAC address to meshId bidirectionally`() {
        // Resolve meshId -> MAC
        val resolvedAddressB = routingCoordinator.resolvePeerAddress(peerBMeshId)
        assertEquals(peerBMac, resolvedAddressB)

        val resolvedAddressC = routingCoordinator.resolvePeerAddress(peerCMeshId)
        assertEquals(peerCMac, resolvedAddressC)

        // Resolve MAC -> meshId
        val resolvedMeshB = routingCoordinator.resolveMeshId(peerBMac)
        assertEquals(peerBMeshId, resolvedMeshB)

        val resolvedMeshC = routingCoordinator.resolveMeshId(peerCMac)
        assertEquals(peerCMeshId, resolvedMeshC)
    }

    @Test
    fun `BleTransportImpl connectedPeers contains both MAC addresses and resolved meshIds`() {
        every { gattManager.connectedServers } returns emptyMap()
        every { gattManager.activeClients } returns mapOf(
            peerBMac to mockk(relaxed = true),
            peerCMac to mockk(relaxed = true)
        )

        val connectedPeers = bleTransport.connectedPeers

        // Contains both MACs
        assertTrue(connectedPeers.contains(peerBMac))
        assertTrue(connectedPeers.contains(peerCMac))

        // Contains both resolved meshIds
        assertTrue(connectedPeers.contains(peerBMeshId))
        assertTrue(connectedPeers.contains(peerCMeshId))
    }

    @Test
    fun `BleTransportImpl broadcastPacket routes unicast media packet directly to target address instead of broadcast`() = runTest {
        val unicastChunk = MeshPacket(
            packetId = "chunk-123",
            senderId = "local_user",
            targetId = peerBMeshId,
            payload = "encrypted_chunk_data",
            type = PacketType.MEDIA_CHUNK
        )

        // When includeAddress is null, broadcastPacket should resolve targetId to peerBMac
        bleTransport.broadcastPacket(unicastChunk, excludeAddress = null, includeAddress = null)

        verify {
            gattManager.broadcastPacket(
                jsonPacket = any(),
                excludeAddress = null,
                includeAddress = peerBMac
            )
        }
    }

    @Test
    fun `RoutingEngine getNextHopForForwarding matches when connectedNodes has meshId`() {
        val routeManager = mockk<RouteManager>(relaxed = true)
        val routeEntry = RouteEntry(
            destinationId = peerBMeshId,
            nextHop = peerBMeshId,
            hops = 1,
            routeType = RouteType.BLE,
            lastSeen = System.currentTimeMillis()
        )
        every { routeManager.getOptimalRoute(peerBMeshId, any()) } returns routeEntry

        val routingEngine = RoutingEngine(
            routeManager = routeManager,
            qosManager = mockk(relaxed = true),
            congestionMonitor = mockk(relaxed = true),
            routeHealthMonitor = mockk(relaxed = true),
            topologyEngine = mockk(relaxed = true),
            batteryAwareNetworking = mockk(relaxed = true),
            transportManager = mockk(relaxed = true),
            retryEngine = mockk(relaxed = true),
            queueOptimizer = mockk(relaxed = true),
            routeOptimizer = mockk(relaxed = true),
            configManager = mockk(relaxed = true)
        )

        val connectedNodes = setOf(peerBMac, peerBMeshId, peerCMac, peerCMeshId)
        val packet = MeshPacket(
            packetId = "meta-1",
            senderId = "local_user",
            targetId = peerBMeshId,
            payload = "meta_payload",
            type = PacketType.MEDIA_META
        )

        val nextHop = routingEngine.getNextHopForForwarding(packet, connectedNodes, excludeHop = "")
        assertEquals(peerBMeshId, nextHop)
    }

    @Test
    fun `RoutingEngine getNextHopForForwarding matches targetId directly if no optimalRoute but connected`() {
        val routeManager = mockk<RouteManager>(relaxed = true)
        every { routeManager.getOptimalRoute(any(), any()) } returns null

        val routingEngine = RoutingEngine(
            routeManager = routeManager,
            qosManager = mockk(relaxed = true),
            congestionMonitor = mockk(relaxed = true),
            routeHealthMonitor = mockk(relaxed = true),
            topologyEngine = mockk(relaxed = true),
            batteryAwareNetworking = mockk(relaxed = true),
            transportManager = mockk(relaxed = true),
            retryEngine = mockk(relaxed = true),
            queueOptimizer = mockk(relaxed = true),
            routeOptimizer = mockk(relaxed = true),
            configManager = mockk(relaxed = true)
        )

        val connectedNodes = setOf(peerBMac, peerBMeshId, peerCMac, peerCMeshId)
        val packet = MeshPacket(
            packetId = "chunk-1",
            senderId = "local_user",
            targetId = peerBMeshId,
            payload = "chunk_payload",
            type = PacketType.MEDIA_CHUNK
        )

        val nextHop = routingEngine.getNextHopForForwarding(packet, connectedNodes, excludeHop = "")
        assertEquals(peerBMeshId, nextHop)
    }

    @Test
    fun `GattWriteQueue allows dequeuing ready fragment for Device C when Device B has an active write`() {
        val queue = GattWriteQueueImpl()

        val writeB = PendingClientWrite(peerBMac, byteArrayOf(1, 2, 3))
        val writeC = PendingClientWrite(peerCMac, byteArrayOf(4, 5, 6))

        queue.enqueue(writeB)
        queue.enqueue(writeC)

        val now = System.currentTimeMillis()

        // Dequeue first for Device B and mark active
        val dequeuedB = queue.dequeueReady(now) { true }
        assertNotNull(dequeuedB)
        assertEquals(peerBMac, dequeuedB?.address)
        queue.setActiveWrite(dequeuedB)

        // Device B is now active
        assertEquals(peerBMac, queue.getActiveWriteAddress())
        assertTrue(queue.hasPendingForDevice(peerBMac))

        // Dequeue for Device C while Device B is active
        val dequeuedC = queue.dequeueReady(now) { true }
        assertNotNull(dequeuedC)
        assertEquals(peerCMac, dequeuedC?.address)
        queue.setActiveWrite(dequeuedC)

        // Both Device B and Device C have active writes (one per device connection)
        assertTrue(queue.hasPendingForDevice(peerBMac))
        assertTrue(queue.hasPendingForDevice(peerCMac))
    }
}
