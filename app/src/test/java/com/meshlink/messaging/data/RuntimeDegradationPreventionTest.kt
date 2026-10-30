package com.meshlink.messaging.data

import android.content.Context
import com.meshlink.ble.api.BleTransport
import com.meshlink.ble.data.BleConnectionManager
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.ble.data.DiscoveryManager
import com.meshlink.ble.data.RoutingCoordinator
import com.meshlink.ble.data.gatt.ApplicationMessageQueueImpl
import com.meshlink.ble.data.gatt.GattWriteQueueImpl
import com.meshlink.ble.data.gatt.PendingApplicationMessage
import com.meshlink.ble.data.gatt.PendingClientWrite
import com.meshlink.ble.discovery.DiscoveryCache
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.ble.discovery.PeerDiscoveryRecord
import com.meshlink.ble.discovery.PeerLifecycleState
import com.meshlink.database.data.local.RelayDao
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.BroadcastType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.data.MeshRouter
import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.routing.engine.IntelligentTransportManager
import com.meshlink.routing.engine.MeshTopologyManager
import com.meshlink.routing.engine.QueueOptimizer
import com.meshlink.routing.engine.RoutingEngine
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import com.meshlink.wifi.api.WifiTransport
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification test suite for Issue 2:
 * 15–20 Minute Runtime Degradation Prevention.
 *
 * Verifies that:
 * 1. GATT write queue recovers from dropped BLE callbacks without queue deadlock.
 * 2. Application message queue prioritizes text and ACKs over bulk media chunks (no head-of-line blocking).
 * 3. Mesh router permits legitimate unicast retries through intermediate relays without 10-minute duplicate poisoning.
 * 4. Rotated BLE MAC addresses (15-min RPA rotation) resolve to the freshest active link.
 * 5. BleScannerManager does not falsely report stopped scanner during scheduled idle duty cycles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RuntimeDegradationPreventionTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testGattWriteQueueRecoversFromDroppedBleCallbacksWithoutDeadlock() {
        val writeQueue = GattWriteQueueImpl()
        var virtualClock = 10_000L
        writeQueue.clock = { virtualClock }

        val peerAddress = "11:22:33:44:55:66"
        val writeChunk1 = PendingClientWrite(peerAddress, byteArrayOf(1, 2, 3))
        val writeChunk2 = PendingClientWrite(peerAddress, byteArrayOf(4, 5, 6))

        writeQueue.enqueue(writeChunk1)
        writeQueue.enqueue(writeChunk2)

        // Dequeue first write
        val dequeued1 = writeQueue.dequeueReady(virtualClock) { true }
        assertNotNull(dequeued1)
        assertEquals(writeChunk1, dequeued1)

        // Start write: active write is set, simulating Android BLE stack processing
        writeQueue.setActiveWrite(dequeued1)
        assertTrue(writeQueue.hasPendingForDevice(peerAddress))

        // While active write is in flight, subsequent writes for that peer are blocked
        virtualClock += 500L
        val blocked = writeQueue.dequeueReady(virtualClock) { true }
        assertNull(blocked)

        // Simulate DROPPED onCharacteristicWrite: callback never arrives from Android Bluetooth stack.
        // Advance clock past the 3,000ms watchdog timeout threshold
        virtualClock += GattWriteQueueImpl.ACTIVE_WRITE_TIMEOUT_MS + 200L

        // Verify writeQueue automatically prunes the stuck active write and allows writeChunk2 to proceed
        val dequeued2 = writeQueue.dequeueReady(virtualClock) { true }
        assertNotNull(dequeued2)
        assertEquals(writeChunk2, dequeued2)

        // Mark writeChunk2 active and then completed normally via removeActive
        writeQueue.setActiveWrite(dequeued2)
        val removed = writeQueue.removeActive(peerAddress)
        assertEquals(writeChunk2, removed)
        assertFalse(writeQueue.hasPendingForDevice(peerAddress))
    }

    @Test
    fun testApplicationMessageQueuePrioritizesTextAndAckOverBulkMediaChunks() {
        val appQueue = ApplicationMessageQueueImpl()
        val peerAddress = "AA:BB:CC:DD:EE:FF"

        // Enqueue bulk media chunks first (priority 10 and priority 0)
        val mediaChunk1 = PendingApplicationMessage(peerAddress, "chunk1".toByteArray(), priority = 10)
        val mediaChunk2 = PendingApplicationMessage(peerAddress, "chunk2".toByteArray(), priority = 0)
        appQueue.enqueue(mediaChunk1)
        appQueue.enqueue(mediaChunk2)

        // User sends a text message during active media transfer (priority 22)
        val textMessage = PendingApplicationMessage(peerAddress, "Hello Mesh".toByteArray(), priority = 22)
        appQueue.enqueue(textMessage)

        // System sends an urgent delivery ACK (priority 34)
        val deliveryAck = PendingApplicationMessage(peerAddress, "ACK:123".toByteArray(), priority = 34)
        appQueue.enqueue(deliveryAck)

        // Dequeue order must be: DELIVERY_ACK (34) -> TEXT (22) -> MEDIA_CHUNK_1 (10) -> MEDIA_CHUNK_2 (0)
        val first = appQueue.dequeueReady(peerAddress)
        assertNotNull(first)
        assertEquals(deliveryAck, first)

        val second = appQueue.dequeueReady(peerAddress)
        assertNotNull(second)
        assertEquals(textMessage, second)

        val third = appQueue.dequeueReady(peerAddress)
        assertNotNull(third)
        assertEquals(mediaChunk1, third)

        val fourth = appQueue.dequeueReady(peerAddress)
        assertNotNull(fourth)
        assertEquals(mediaChunk2, fourth)

        assertNull(appQueue.dequeueReady(peerAddress))
    }

    @Test
    fun testApplicationMessageQueueMaintainsFifoForEqualPriority() {
        val appQueue = ApplicationMessageQueueImpl()
        val peerAddress = "AA:BB:CC:DD:EE:FF"

        val text1 = PendingApplicationMessage(peerAddress, "Msg 1".toByteArray(), priority = 22)
        val text2 = PendingApplicationMessage(peerAddress, "Msg 2".toByteArray(), priority = 22)
        val text3 = PendingApplicationMessage(peerAddress, "Msg 3".toByteArray(), priority = 22)

        appQueue.enqueue(text1)
        appQueue.enqueue(text2)
        appQueue.enqueue(text3)

        assertEquals(text1, appQueue.dequeueReady(peerAddress))
        assertEquals(text2, appQueue.dequeueReady(peerAddress))
        assertEquals(text3, appQueue.dequeueReady(peerAddress))
        assertNull(appQueue.dequeueReady(peerAddress))
    }

    @Test
    fun testMeshRouterRelaysRetriedUnicastPacketsAfterTransientFailure() = runTest {
        val bleTransport = mockk<BleTransport>(relaxed = true)
        val wifiTransport = mockk<WifiTransport>(relaxed = true)
        val relayDao = mockk<RelayDao>(relaxed = true)
        val trustManager = mockk<TrustManager>(relaxed = true)
        val routingEngine = mockk<RoutingEngine>(relaxed = true)
        val topologyManager = mockk<MeshTopologyManager>(relaxed = true)
        val queueOptimizer = mockk<QueueOptimizer>(relaxed = true)
        val congestionMonitor = mockk<CongestionMonitor>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val transportManager = mockk<IntelligentTransportManager>(relaxed = true)

        val bleIncoming = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 100)
        val wifiIncoming = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 100)

        every { settingsRepository.advancedEncryptionEnforcement } returns flowOf(true)
        every { settingsRepository.isMeshRelayEnabled } returns flowOf(true)
        every { settingsRepository.meshMaxHops } returns flowOf(5)
        every { settingsRepository.meshTtl } returns flowOf(10)
        every { bleTransport.incomingPackets } returns bleIncoming
        every { wifiTransport.incomingPackets } returns wifiIncoming
        every { trustManager.getTrustLevel(any()) } returns TrustLevel.TRUSTED
        every { routingEngine.queueOptimizer } returns queueOptimizer
        every { routingEngine.congestionMonitor } returns congestionMonitor
        every { routingEngine.transportManager } returns transportManager
        every { bleTransport.connectedPeers } returns setOf("NODE_C")
        every { wifiTransport.connectedPeers } returns emptySet()

        val queuedPackets = mutableListOf<MeshPacket>()
        every { queueOptimizer.enqueue(capture(queuedPackets)) } just Runs
        every { routingEngine.getNextHopForForwarding(any(), any(), any()) } returns "NODE_C"
        every { routingEngine.isRoutingLoop(any(), any()) } returns false

        // Duplicate cache mock: first time true (new), second time false (already seen)
        val seenPacketIds = mutableSetOf<String>()
        every { routingEngine.markPacketProcessed(any()) } answers {
            val id = firstArg<String>()
            val isNew = seenPacketIds.add(id)
            isNew
        }

        val router = MeshRouter(
            bleTransport = bleTransport,
            wifiTransport = wifiTransport,
            relayDao = relayDao,
            trustManager = trustManager,
            routingEngine = routingEngine,
            topologyManager = topologyManager,
            settingsRepository = settingsRepository,
            userRepositoryProvider = null,
            applicationScope = backgroundScope
        )
        router.localMeshId = "NODE_B"
        testScheduler.runCurrent() // Start incoming packet collector coroutine

        val unicastPacket = MeshPacket(
            packetId = "MSG_1001",
            senderId = "NODE_A",
            targetId = "NODE_C",
            payload = "Hello via relay",
            type = PacketType.TEXT,
            encrypted = true,
            ttl = 5,
            hopCount = 0
        )

        // 1. First reception: Node B relays packet to Node C
        bleIncoming.emit("NODE_A" to unicastPacket)
        testScheduler.runCurrent()
        assertEquals(1, queuedPackets.size)
        assertEquals("MSG_1001", queuedPackets[0].packetId)

        // 2. Immediate duplicate (e.g. multi-path echo <1000ms): dropped
        bleIncoming.emit("NODE_A" to unicastPacket)
        testScheduler.runCurrent()
        assertEquals(1, queuedPackets.size) // No extra enqueue

        // 3. Sender retries after transmission timeout (>1000ms): must NOT be dropped by 10-minute duplicate cache
        Thread.sleep(1050L)
        bleIncoming.emit("NODE_A" to unicastPacket)
        testScheduler.runCurrent()
        assertEquals(2, queuedPackets.size)
        assertEquals("MSG_1001", queuedPackets[1].packetId)

        // 4. Broadcast packet duplicate: must always be dropped
        val broadcastPacket = MeshPacket(
            packetId = "BCAST_2002",
            senderId = "NODE_A",
            targetId = "BROADCAST",
            payload = """{"msg":"all"}""",
            type = PacketType.TEXT,
            broadcastType = BroadcastType.GLOBAL,
            ttl = 5,
            hopCount = 1
        )
        bleIncoming.emit("NODE_A" to broadcastPacket)
        testScheduler.runCurrent()
        Thread.sleep(1050L)
        bleIncoming.emit("NODE_A" to broadcastPacket)
        testScheduler.runCurrent()
        // Broadcast packets do not forward across multiple hops and duplicate broadcasts are dropped
        assertEquals(2, queuedPackets.size)
    }

    @Test
    fun testRoutingCoordinatorResolvesFreshestMacWhenPeerMacRotates() {
        val userRepo = mockk<UserRepository>(relaxed = true)
        val connManager = mockk<BleConnectionManager>(relaxed = true)
        val discoveryManager = mockk<DiscoveryManager>(relaxed = true)
        val discoveryEngine = mockk<DiscoveryEngine>(relaxed = true)
        val discoveryCache = mockk<DiscoveryCache>(relaxed = true)
        val router = mockk<Router>(relaxed = true)

        every { discoveryManager.discoveryEngine } returns discoveryEngine
        every { discoveryEngine.cache } returns discoveryCache
        every { connManager.activeClients } returns emptySet()
        every { router.routeTable } returns emptyMap()

        val peerMeshId = "PEER_ALICE"
        val oldRotatedMac = "AA:BB:CC:11:22:33"
        val newRotatedMac = "DD:EE:FF:44:55:66"

        // Cache contains old record (timestamp 1,000ms) and new rotated record (timestamp 900,000ms = 15m later)
        val oldRecord = PeerDiscoveryRecord(
            macAddress = oldRotatedMac,
            meshId = peerMeshId,
            name = "Alice",
            lastSeenMillis = 1000L,
            linkActive = false
        )
        val newRecord = PeerDiscoveryRecord(
            macAddress = newRotatedMac,
            meshId = peerMeshId,
            name = "Alice",
            lastSeenMillis = 900_000L,
            linkActive = true
        )

        every { discoveryCache.getAll() } returns listOf(oldRecord, newRecord)
        every { discoveryManager.scannedDevices } returns MutableStateFlow(
            mapOf(
                newRotatedMac to BleDevice(
                    meshId = peerMeshId,
                    name = "Alice",
                    address = newRotatedMac,
                    rssi = -60,
                    lastSeen = 900_000L,
                    capabilities = 0,
                    isConnected = true
                )
            )
        )

        val coordinator = RoutingCoordinator(
            userRepository = userRepo,
            cryptoManager = mockk(relaxed = true),
            trustManager = mockk(relaxed = true),
            sessionManager = mockk(relaxed = true),
            rekeyManager = mockk(relaxed = true),
            meshRouter = router,
            connectionManager = connManager,
            discoveryManager = discoveryManager
        )

        val resolvedMac = coordinator.resolvePeerAddress(peerMeshId)
        assertEquals("Must resolve to freshest/active MAC after 15m rotation", newRotatedMac, resolvedMac)
    }

    @Test
    fun testBleScannerReportsScanningActiveDuringDutyCycleIdlePhase() {
        val context = mockk<Context>(relaxed = true)
        val discoveryEngine = mockk<DiscoveryEngine>(relaxed = true)
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)

        // Simulate engine running, but in scheduled duty-cycle idle phase (hardware scanCallback null)
        every { discoveryEngine.isScanning } returns true

        val scannerManager = BleScannerManager(
            context = context,
            discoveryEngine = discoveryEngine,
            settingsRepository = settingsRepo,
            applicationScope = testScope,
            permissionChecker = mockk(relaxed = true),
            restartCoordinator = mockk(relaxed = true)
        )

        // Must report true to prevent MeshWatchdog false-alarm restarts
        assertTrue("Scanner must report active when discovery engine is running", scannerManager.isScanning)
    }
}
