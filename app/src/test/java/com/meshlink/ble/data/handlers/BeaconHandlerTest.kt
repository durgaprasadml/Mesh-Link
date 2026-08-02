package com.meshlink.ble.data.handlers

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteType
import com.meshlink.routing.engine.MeshTopologyManager
import com.meshlink.routing.engine.RouteManager
import com.meshlink.routing.engine.RoutingTable
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BeaconHandlerTest {

    private val topologyManager = mockk<MeshTopologyManager>(relaxed = true)
    private val routeManager = mockk<RouteManager>(relaxed = true)
    private val routingTable = mockk<RoutingTable>(relaxed = true)

    private lateinit var beaconHandler: BeaconHandler

    @Before
    fun setUp() {
        beaconHandler = BeaconHandler(
            topologyManager = topologyManager,
            routeManager = routeManager,
            routingTable = routingTable
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `generateBeaconPacket creates valid BEACON packet with local routes`() {
        val nodeB = com.meshlink.util.MeshIdNormalizer.canonicalize("node_b")
        val nodeA = com.meshlink.util.MeshIdNormalizer.canonicalize("node_a")
        val route1 = RouteEntry(nodeB, nodeB, 1, RouteType.BLE)
        every { routingTable.getAllRoutes() } returns mapOf(nodeB to listOf(route1))

        val beacon = beaconHandler.generateBeaconPacket("node_a")

        assertEquals(nodeA, beacon.senderId)
        assertEquals("BROADCAST", beacon.targetId)
        assertEquals(PacketType.BEACON, beacon.type)
        assertFalse(beacon.encrypted)
        assertTrue(beacon.payload.contains(nodeB))
    }

    @Test
    fun `handleBeaconPacket parses topology payload and updates routes`() {
        val nodeB = com.meshlink.util.MeshIdNormalizer.canonicalize("node_b")
        val nodeC = com.meshlink.util.MeshIdNormalizer.canonicalize("node_c")
        val payload = """
            {
                "nodeId": "$nodeB",
                "reachable": [
                    {"nodeId": "$nodeC", "hops": 1, "transport": "BLE"}
                ],
                "timestamp": 100000
            }
        """.trimIndent()

        val packet = MeshPacket(
            senderId = nodeB,
            targetId = "BROADCAST",
            payload = payload,
            type = PacketType.BEACON
        )

        beaconHandler.handleBeaconPacket(packet)

        verify { topologyManager.updateNeighbor(nodeB, rssi = -65, transport = RouteType.BLE) }
        verify { routeManager.updateRoute(nodeB, nodeB, 1, rssi = -65, trustScore = 50, type = RouteType.BLE) }
        verify { routeManager.updateRoute(nodeC, nodeB, 2, rssi = -70, trustScore = 50, type = RouteType.BLE) }
        verify { topologyManager.updateTopology(nodeB, listOf(nodeC)) }
        verify { topologyManager.recomputeTopology() }
    }
}
