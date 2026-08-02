package com.meshlink.routing.engine

import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MeshTopologyManagerTest {

    private lateinit var routingTable: RoutingTable
    private lateinit var topologyManager: MeshTopologyManager

    @Before
    fun setUp() {
        routingTable = RoutingTable()
        topologyManager = MeshTopologyManager(routingTable)
    }

    @Test
    fun `direct neighbor update updates topology metrics`() {
        topologyManager.updateNeighbor("Peer_B", rssi = -60, transport = RouteType.BLE)

        val metrics = topologyManager.metrics.value
        assertEquals(1, metrics.directNeighbors)
    }

    @Test
    fun `multi-hop reachable node is correctly added and computed`() {
        // A -> B -> C (C is 2 hops from A via B)
        routingTable.addOrUpdateRoute(
            RouteEntry(
                destinationId = "Peer_C",
                nextHop = "Peer_B",
                hops = 2,
                score = 80
            )
        )

        topologyManager.recomputeTopology()

        val reachable = topologyManager.reachableNodes.value
        assertEquals(1, reachable.size)
        assertEquals("Peer_C", reachable.first().nodeId)
        assertEquals("Peer_B", reachable.first().viaRelayId)
        assertEquals(2, reachable.first().hopCount)
    }

    @Test
    fun `removing neighbor invalidates routes via that relay and triggers recovery event`() {
        routingTable.addOrUpdateRoute(
            RouteEntry(
                destinationId = "Peer_C",
                nextHop = "Peer_B",
                hops = 2,
                score = 80
            )
        )
        topologyManager.updateNeighbor("Peer_B", rssi = -60)
        topologyManager.recomputeTopology()

        assertEquals(1, topologyManager.reachableNodes.value.size)

        // Peer B goes offline
        topologyManager.removeNeighbor("Peer_B")

        assertEquals(0, topologyManager.reachableNodes.value.size)
        assertEquals(1, topologyManager.metrics.value.recoveryCount)
    }
}
