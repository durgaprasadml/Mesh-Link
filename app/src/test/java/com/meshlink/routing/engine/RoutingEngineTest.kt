package com.meshlink.routing.engine

import com.meshlink.config.RuntimeConfigManager
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutingEngineTest {

    private val routeManager = mockk<RouteManager>(relaxed = true)
    private val qosManager = mockk<QoSManager>(relaxed = true)
    private val congestionMonitor = mockk<CongestionMonitor>(relaxed = true)
    private val routeHealthMonitor = mockk<RouteHealthMonitor>(relaxed = true)
    private val topologyEngine = mockk<NetworkTopologyEngine>(relaxed = true)
    private val batteryAwareNetworking = mockk<BatteryAwareNetworking>(relaxed = true)
    private val transportManager = mockk<IntelligentTransportManager>(relaxed = true)
    private val retryEngine = mockk<IntelligentRetryEngine>(relaxed = true)
    private val queueOptimizer = mockk<QueueOptimizer>(relaxed = true)
    private val discoveryEngine = mockk<RouteDiscoveryEngine>(relaxed = true)
    private val repairManager = mockk<RouteRepairManager>(relaxed = true)
    private val routeOptimizer = mockk<RouteOptimizer>(relaxed = true)
    private val configManager = mockk<RuntimeConfigManager>(relaxed = true)

    private lateinit var routingEngine: RoutingEngine

    @Before
    fun setUp() {
        every { configManager.currentConfig.value } returns mockk(relaxed = true) {
            every { duplicateCacheSize } returns 1000
            every { duplicateCacheLifetimeMs } returns 60_000L
        }

        routingEngine = RoutingEngine(
            routeManager = routeManager,
            qosManager = qosManager,
            congestionMonitor = congestionMonitor,
            routeHealthMonitor = routeHealthMonitor,
            topologyEngine = topologyEngine,
            batteryAwareNetworking = batteryAwareNetworking,
            transportManager = transportManager,
            retryEngine = retryEngine,
            queueOptimizer = queueOptimizer,
            discoveryEngine = discoveryEngine,
            repairManager = repairManager,
            routeOptimizer = routeOptimizer,
            configManager = configManager
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `markPacketProcessed returns true for new packet and false for duplicate`() {
        val isNew1 = routingEngine.markPacketProcessed("pkt_100")
        val isNew2 = routingEngine.markPacketProcessed("pkt_100")

        assertTrue(isNew1)
        assertFalse(isNew2)
    }

    @Test
    fun `isRoutingLoop returns true if TTL is zero or less`() {
        val packet = MeshPacket(
            packetId = "pkt_loop_1",
            senderId = "node_a",
            targetId = "node_b",
            payload = "Test",
            ttl = 0
        )

        val isLoop = routingEngine.isRoutingLoop(packet, "local_node")
        assertTrue(isLoop)
    }

    @Test
    fun `isRoutingLoop returns true if localMeshId is in visitedPath`() {
        val packet = MeshPacket(
            packetId = "pkt_loop_2",
            senderId = "node_a",
            targetId = "node_b",
            payload = "Test",
            ttl = 5,
            visitedPath = listOf("node_x", "local_node", "node_y")
        )

        val isLoop = routingEngine.isRoutingLoop(packet, "local_node")
        assertTrue(isLoop)
    }

    @Test
    fun `isRoutingLoop returns false for valid unvisited packet with positive TTL`() {
        val packet = MeshPacket(
            packetId = "pkt_valid",
            senderId = "node_a",
            targetId = "node_b",
            payload = "Test",
            ttl = 5,
            visitedPath = listOf("node_x", "node_y")
        )

        val isLoop = routingEngine.isRoutingLoop(packet, "local_node")
        assertFalse(isLoop)
    }

    @Test
    fun `start and stop delegate to routeHealthMonitor`() {
        routingEngine.start()
        verify { routeHealthMonitor.start() }

        routingEngine.stop()
        verify { routeHealthMonitor.stop() }
    }
}
