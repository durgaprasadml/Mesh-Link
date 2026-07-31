package com.meshlink.routing.data

import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.ble.api.BleTransport
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.RelayPacketEntity
import com.meshlink.domain.model.DispatchResult
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.routing.api.PacketQueued
import com.meshlink.routing.engine.QueueOptimizer
import com.meshlink.routing.engine.RoutingEngine
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshRouterTest {

    private val hybridTransport = mockk<com.meshlink.transport.HybridTransport>(relaxed = true)
    private val relayDao = mockk<RelayDao>(relaxed = true)
    private val trustManager = mockk<TrustManager>(relaxed = true)
    private val routingEngine = mockk<RoutingEngine>(relaxed = true)
    private val queueOptimizer = mockk<QueueOptimizer>(relaxed = true)
    private val congestionMonitor = mockk<CongestionMonitor>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private lateinit var incomingPacketsFlow: MutableSharedFlow<Pair<String, MeshPacket>>

    @Before
    fun setUp() {
        incomingPacketsFlow = MutableSharedFlow(extraBufferCapacity = 100)

        every { settingsRepository.advancedEncryptionEnforcement } returns flowOf(true)
        every { settingsRepository.isMeshRelayEnabled } returns flowOf(true)
        every { settingsRepository.meshMaxHops } returns flowOf(5)
        every { settingsRepository.meshTtl } returns flowOf(10)
        every { hybridTransport.incomingPackets } returns incomingPacketsFlow
        every { trustManager.getTrustLevel(any()) } returns TrustLevel.TRUSTED
        every { routingEngine.queueOptimizer } returns queueOptimizer
        every { routingEngine.congestionMonitor } returns congestionMonitor
        every { queueOptimizer.size() } returns 0
        every { routingEngine.markPacketProcessed(any()) } returns true
        every { routingEngine.isRoutingLoop(any(), any()) } returns false
        coEvery { relayDao.insertPacket(any()) } just Runs
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createRouter(scope: CoroutineScope): MeshRouter {
        val router = MeshRouter(
            hybridTransport = hybridTransport,
            relayDao = relayDao,
            trustManager = trustManager,
            routingEngine = routingEngine,
            settingsRepository = settingsRepository,
            applicationScope = scope
        )
        router.localMeshId = "node_local"
        return router
    }

    @Test
    fun `routePayload enqueues packet into routingEngine and emits PacketQueued event`() = runTest {
        val meshRouter = createRouter(backgroundScope)
        val eventDeferred = async { meshRouter.packetEvents.first() }
        testScheduler.runCurrent()

        val result = meshRouter.routePayload(
            targetId = "node_target",
            payload = "Hello World",
            myAddressAlias = "node_local",
            encrypted = true,
            packetId = "pkt_001"
        )
        testScheduler.advanceUntilIdle()

        assertTrue(result is DispatchResult.Queued)
        verify { routingEngine.markPacketProcessed("pkt_001") }
        verify { queueOptimizer.enqueue(match { it.packetId == "pkt_001" && it.targetId == "node_target" }) }
        
        val event = eventDeferred.await()
        assertTrue(event is PacketQueued)
        assertEquals("pkt_001", (event as PacketQueued).packetId)
    }

    @Test
    fun `handleIncomingPacket delivers packet locally when target matches localMeshId`() = runTest {
        val meshRouter = createRouter(backgroundScope)
        val payloadDeferred = async { meshRouter.incomingPayloads.first() }
        testScheduler.runCurrent()

        val packet = MeshPacket(
            packetId = "pkt_100",
            senderId = "node_sender",
            targetId = "node_local",
            payload = "Secret Message",
            encrypted = true,
            ttl = 5
        )

        incomingPacketsFlow.emit("peer_address_1" to packet)
        testScheduler.advanceUntilIdle()

        val (sender, receivedPacket) = payloadDeferred.await()
        assertEquals("node_sender", sender)
        assertEquals("pkt_100", receivedPacket.packetId)
    }

    @Test
    fun `handleIncomingPacket drops packet if sender is blocked`() = runTest {
        every { trustManager.getTrustLevel("rogue_node") } returns TrustLevel.BLOCKED
        val meshRouter = createRouter(backgroundScope)
        val receivedPayloads = mutableListOf<Pair<String, MeshPacket>>()
        backgroundScope.launch {
            meshRouter.incomingPayloads.collect { receivedPayloads.add(it) }
        }
        testScheduler.runCurrent()

        val packet = MeshPacket(
            packetId = "pkt_blocked",
            senderId = "rogue_node",
            targetId = "node_local",
            payload = "Malicious Payload",
            encrypted = true,
            ttl = 5
        )

        incomingPacketsFlow.emit("peer_address_1" to packet)
        testScheduler.advanceUntilIdle()

        assertTrue(receivedPayloads.isEmpty())
    }

    @Test
    fun `handleIncomingPacket drops duplicate non-local packet`() = runTest {
        every { routingEngine.markPacketProcessed("pkt_dup") } returns false
        val meshRouter = createRouter(backgroundScope)
        val receivedPayloads = mutableListOf<Pair<String, MeshPacket>>()
        backgroundScope.launch {
            meshRouter.incomingPayloads.collect { receivedPayloads.add(it) }
        }
        testScheduler.runCurrent()

        val packet = MeshPacket(
            packetId = "pkt_dup",
            senderId = "node_sender",
            targetId = "node_other",
            payload = "Relayed Data",
            encrypted = true,
            ttl = 5
        )

        incomingPacketsFlow.emit("peer_address_1" to packet)
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { queueOptimizer.enqueue(match { it.packetId == "pkt_dup" }) }
        assertTrue(receivedPayloads.isEmpty())
    }

    @Test
    fun `handleIncomingPacket stores packet in RelayDao when no peers available to forward`() = runTest {
        every { hybridTransport.connectedPeers } returns emptySet()
        createRouter(backgroundScope)
        testScheduler.runCurrent()

        val packet = MeshPacket(
            packetId = "pkt_store",
            senderId = "node_sender",
            targetId = "node_target_away",
            payload = "Store and forward payload",
            encrypted = true,
            ttl = 4
        )

        incomingPacketsFlow.emit("peer_address_1" to packet)
        testScheduler.advanceUntilIdle()
        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        coVerify {
            relayDao.insertPacket(any())
        }
    }

    @Test
    fun `routeMediaPacket enqueues media packet and emits queued status`() = runTest {
        val meshRouter = createRouter(backgroundScope)
        val mediaPacket = MeshPacket(
            packetId = "media_001",
            senderId = "node_local",
            targetId = "node_remote",
            payload = "base64image",
            type = PacketType.MEDIA_META,
            mimeType = "image/jpeg",
            encrypted = true
        )

        val result = meshRouter.routeMediaPacket(mediaPacket)
        testScheduler.advanceUntilIdle()

        assertTrue(result is DispatchResult.Queued)
        verify { queueOptimizer.enqueue(mediaPacket) }
    }
}
