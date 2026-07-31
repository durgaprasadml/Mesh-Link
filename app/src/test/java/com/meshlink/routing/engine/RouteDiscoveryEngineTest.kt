package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteDiscoveryEngineTest {

    private val routeCache = mockk<RouteCache>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var discoveryEngine: RouteDiscoveryEngine

    @Before
    fun setUp() {
        discoveryEngine = RouteDiscoveryEngine(
            routeCache = routeCache,
            applicationScope = testScope
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `queueAndDiscover broadcasts RREQ when no active discovery exists`() = testScope.runTest {
        val sentPackets = mutableListOf<MeshPacket>()
        every { routeCache.getRoutesForDestination("node_target") } returns emptyList()

        discoveryEngine.queueAndDiscover(
            targetId = "node_target",
            packet = null,
            localMeshId = "node_local",
            sendPacketAction = { sentPackets.add(it) }
        )

        assertTrue(sentPackets.isNotEmpty())
        val rreq = sentPackets.first()
        assertEquals(PacketType.ROUTE_REQUEST, rreq.type)
        assertEquals("node_target", rreq.targetId)
        assertEquals("node_local", rreq.senderId)
    }

    @Test
    fun `handleRouteRequest learns reverse route and replies with RREP when target matches local node`() = testScope.runTest {
        val sentReplies = mutableListOf<Pair<MeshPacket, String?>>()
        every { routeCache.getRoutesForDestination("node_sender") } returns emptyList()

        val rreq = MeshPacket(
            packetId = "pkt_rreq_1",
            senderId = "node_sender",
            targetId = "node_local",
            payload = "RREQ:node_local",
            type = PacketType.ROUTE_REQUEST,
            ttl = 5,
            hopCount = 1,
            visitedPath = listOf("node_sender"),
            sequenceNumber = 42L
        )

        discoveryEngine.handleRouteRequest(
            immediateSender = "peer_mac_1",
            packet = rreq,
            localMeshId = "node_local",
            sendPacketAction = { p, target -> sentReplies.add(p to target) }
        )

        // Verifies reverse route learned
        verify { routeCache.addOrUpdateRoute(match { it.destinationId == "node_sender" && it.nextHop == "peer_mac_1" }) }

        // Verifies RREP unicast replied
        assertTrue(sentReplies.isNotEmpty())
        val (rrep, target) = sentReplies.first()
        assertEquals(PacketType.ROUTE_REPLY, rrep.type)
        assertEquals("node_local", rrep.senderId)
        assertEquals("node_sender", rrep.targetId)
        assertEquals("peer_mac_1", target)
    }

    @Test
    fun `handleRouteReply flushes pending packets when reaching origin destination`() = testScope.runTest {
        val flushedPackets = mutableListOf<MeshPacket>()
        every { routeCache.getRoutesForDestination(any()) } returns emptyList()

        // Queue a pending packet
        val testPacket = MeshPacket(
            packetId = "pkt_data_1",
            senderId = "node_local",
            targetId = "node_target",
            payload = "Hello RREP"
        )

        discoveryEngine.queueAndDiscover(
            targetId = "node_target",
            packet = testPacket,
            localMeshId = "node_local",
            sendPacketAction = {}
        )

        val rrep = MeshPacket(
            packetId = "pkt_rrep_1",
            senderId = "node_target",
            targetId = "node_local",
            payload = "RREP:node_target",
            type = PacketType.ROUTE_REPLY,
            ttl = 5,
            hopCount = 2,
            visitedPath = listOf("node_target", "intermediate"),
            sequenceNumber = 10L
        )

        discoveryEngine.handleRouteReply(
            immediateSender = "intermediate",
            packet = rrep,
            localMeshId = "node_local",
            sendPacketAction = { _, _ -> },
            flushPendingAction = { _, list -> flushedPackets.addAll(list) }
        )

        assertTrue(flushedPackets.isNotEmpty())
        assertEquals("pkt_data_1", flushedPackets.first().packetId)
    }
}
