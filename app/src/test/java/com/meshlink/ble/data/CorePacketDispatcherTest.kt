package com.meshlink.ble.data

import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.DispatchResult
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CorePacketDispatcherTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val cryptoManager = mockk<MeshCryptoManager>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val meshRouter = mockk<Router>(relaxed = true)
    private val routingCoordinator = mockk<RoutingCoordinator>(relaxed = true)
    private val connectionManager = mockk<BleConnectionManager>(relaxed = true)
    private val discoveryManager = mockk<DiscoveryManager>(relaxed = true)

    private val scannedDevicesFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())

    private lateinit var dispatcher: CorePacketDispatcher

    private val targetPeerId = "PEER_TARGET"
    private val targetMac = "AA:BB:CC:DD:EE:01"
    private val bystanderMac1 = "AA:BB:CC:DD:EE:02"
    private val bystanderMac2 = "AA:BB:CC:DD:EE:03"

    @Before
    fun setup() {
        every { userRepository.isEncryptionEnabled } returns flowOf(false)
        every { discoveryManager.isScanning() } returns true
        every { discoveryManager.isAdvertising() } returns true
        every { discoveryManager.scannedDevices } returns scannedDevicesFlow
        every { connectionManager.activeClients } returns emptySet()
        every { connectionManager.connectedServers } returns emptySet()
        every { connectionManager.isAnyPeerConnected() } returns false
        every { connectionManager.isPeerConnectedOrConnecting(any()) } returns false
        coEvery { meshRouter.routeMediaPacket(any()) } returns DispatchResult.Queued

        scannedDevicesFlow.value = mapOf(
            targetMac to BleDevice(name = "Target", address = targetMac, meshId = targetPeerId, rssi = -55),
            bystanderMac1 to BleDevice(name = "Bystander 1", address = bystanderMac1, meshId = "PEER_2", rssi = -65),
            bystanderMac2 to BleDevice(name = "Bystander 2", address = bystanderMac2, meshId = "PEER_3", rssi = -70)
        )

        dispatcher = CorePacketDispatcher(
            userRepository = userRepository,
            cryptoManager = cryptoManager,
            sessionManager = sessionManager,
            meshRouter = meshRouter,
            routingCoordinator = routingCoordinator,
            connectionManager = connectionManager,
            discoveryManager = discoveryManager
        )
    }

    @Test
    fun `Direct message to known peer connects only to target and not unrelated scanned devices (ML-AUDIT-003)`() = runTest {
        every { routingCoordinator.resolvePeerAddress(targetPeerId) } returns targetMac

        val packet = MeshPacket(
            packetId = "pkt-direct-1",
            senderId = "LOCAL_PEER",
            targetId = targetPeerId,
            payload = """{"text":"Hi"}""",
            type = PacketType.TEXT,
            encrypted = true
        )

        val result = dispatcher.dispatchSinglePacket(targetPeerId, packet)
        assertEquals(DispatchResult.Queued, result)

        // Connects to the resolved target peer
        verify(exactly = 1) { connectionManager.connectToDevice(targetMac, any()) }
        // Must NOT initiate connections to unrelated scanned bystanders
        verify(exactly = 0) { connectionManager.connectToDevice(bystanderMac1, any()) }
        verify(exactly = 0) { connectionManager.connectToDevice(bystanderMac2, any()) }
    }

    @Test
    fun `Repeated packet sends do not trigger duplicate connection attempts while connecting or connected (ML-AUDIT-003)`() = runTest {
        every { routingCoordinator.resolvePeerAddress(targetPeerId) } returns targetMac
        // First call initiates connection; subsequent calls see peer as connecting/connected
        var connected = false
        every { connectionManager.isPeerConnectedOrConnecting(targetMac) } answers { connected }
        every { connectionManager.connectToDevice(targetMac, any()) } answers { connected = true }

        repeat(5) { idx ->
            val packet = MeshPacket(
                packetId = "pkt-$idx",
                senderId = "LOCAL_PEER",
                targetId = targetPeerId,
                payload = """{"text":"Msg $idx"}""",
                type = PacketType.TEXT,
                encrypted = true
            )
            dispatcher.dispatchSinglePacket(targetPeerId, packet)
        }

        // Only 1 connection attempt across 5 rapid packet dispatches
        verify(exactly = 1) { connectionManager.connectToDevice(targetMac, any()) }
        verify(exactly = 0) { connectionManager.connectToDevice(bystanderMac1, any()) }
        verify(exactly = 0) { connectionManager.connectToDevice(bystanderMac2, any()) }
    }

    @Test
    fun `Broadcast and multi-hop fallback remain bounded and rate-limited (ML-AUDIT-003)`() = runTest {
        // Populate 8 scanned devices (exceeding MAX_OPPORTUNISTIC_CONNECTIONS = 5)
        val manyDevices = (1..8).associate { i ->
            val mac = "AA:BB:CC:DD:EE:0$i"
            mac to BleDevice(name = "Peer $i", address = mac, meshId = "MESH_$i", rssi = -50 - i)
        }
        scannedDevicesFlow.value = manyDevices

        // Unknown peer MAC triggers multi-hop opportunistic fallback
        every { routingCoordinator.resolvePeerAddress("UNKNOWN_MULTI_HOP_PEER") } returns null

        val unroutedPacket = MeshPacket(
            packetId = "pkt-multihop",
            senderId = "LOCAL_PEER",
            targetId = "UNKNOWN_MULTI_HOP_PEER",
            payload = """{"text":"Multi-hop"}""",
            type = PacketType.TEXT,
            encrypted = true
        )

        dispatcher.dispatchSinglePacket("UNKNOWN_MULTI_HOP_PEER", unroutedPacket)

        // Opportunistic connections must be bounded by MAX_OPPORTUNISTIC_CONNECTIONS (5), not all 8
        verify(exactly = CorePacketDispatcher.MAX_OPPORTUNISTIC_CONNECTIONS) {
            connectionManager.connectToDevice(any(), eq(false))
        }

        // Once at least one peer is connected, immediate subsequent broadcasts within cooldown do not re-flood
        every { connectionManager.isAnyPeerConnected() } returns true
        val broadcastPacket = MeshPacket(
            packetId = "pkt-bcast",
            senderId = "LOCAL_PEER",
            targetId = "BROADCAST",
            payload = """{"text":"Broadcast"}""",
            type = PacketType.TEXT,
            encrypted = false
        )
        dispatcher.dispatchSinglePacket("BROADCAST", broadcastPacket)

        // Still only the initial 5 calls (cooldown suppressed duplicate scan flood)
        verify(exactly = CorePacketDispatcher.MAX_OPPORTUNISTIC_CONNECTIONS) {
            connectionManager.connectToDevice(any(), eq(false))
        }
    }
}

