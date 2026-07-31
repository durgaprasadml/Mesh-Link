package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteType
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
class RouteRepairManagerTest {

    private val routeCache = mockk<RouteCache>(relaxed = true)
    private val routeOptimizer = mockk<RouteOptimizer>(relaxed = true)
    private val discoveryEngine = mockk<RouteDiscoveryEngine>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repairManager: RouteRepairManager

    @Before
    fun setUp() {
        repairManager = RouteRepairManager(
            routeCache = routeCache,
            routeOptimizer = routeOptimizer,
            discoveryEngine = discoveryEngine,
            applicationScope = testScope
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `handleLinkFailure removes routes via broken hop and broadcasts RERR when no backup exists`() = testScope.runTest {
        val sentPackets = mutableListOf<MeshPacket>()
        val brokenHop = "relay_node_b"

        every { routeCache.getAllDestinations() } returns listOf("dest_node_c")
        every { routeCache.getRoutesForDestination("dest_node_c") } returns listOf(
            RouteEntry(destinationId = "dest_node_c", nextHop = brokenHop, hops = 2)
        )
        every { routeOptimizer.getBackupRoutes("dest_node_c", brokenHop) } returns emptyList()

        repairManager.handleLinkFailure(
            brokenNextHop = brokenHop,
            localMeshId = "node_local",
            sendPacketAction = { p, _ -> sentPackets.add(p) }
        )

        verify { routeCache.removeRoutesViaHop(brokenHop) }
        verify { discoveryEngine.queueAndDiscover("dest_node_c", null, "node_local", any()) }

        assertTrue(sentPackets.isNotEmpty())
        val rerr = sentPackets.first()
        assertEquals(PacketType.ROUTE_ERROR, rerr.type)
        assertTrue(rerr.payload.contains("dest_node_c"))
    }

    @Test
    fun `handleRouteError invalidates matching broken next hops in cache`() {
        val rerrPacket = MeshPacket(
            packetId = "pkt_rerr_1",
            senderId = "upstream_node",
            targetId = "BROADCAST",
            payload = "node_dead_1, node_dead_2",
            type = PacketType.ROUTE_ERROR
        )

        every { routeCache.getRoutesForDestination("node_dead_1") } returns listOf(
            RouteEntry(destinationId = "node_dead_1", nextHop = "broken_peer", hops = 2)
        )

        repairManager.handleRouteError(
            immediateSender = "broken_peer",
            packet = rerrPacket,
            localMeshId = "node_local"
        )

        verify { routeCache.removeRoutesViaHop("broken_peer") }
    }
}
