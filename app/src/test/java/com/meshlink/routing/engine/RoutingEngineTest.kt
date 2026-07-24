package com.meshlink.routing.engine

import com.meshlink.config.RuntimeConfigManager
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class RoutingEngineTest {

    private lateinit var configManager: RuntimeConfigManager
    private lateinit var routingEngine: RoutingEngine

    @Before
    fun setup() {
        configManager = RuntimeConfigManager()
        
        // Mock dependencies
        val routeManager = mock(RouteManager::class.java)
        val qosManager = mock(QoSManager::class.java)
        val congestionMonitor = mock(CongestionMonitor::class.java)
        val routeHealthMonitor = mock(RouteHealthMonitor::class.java)
        val topologyEngine = mock(NetworkTopologyEngine::class.java)
        val batteryAwareNetworking = mock(BatteryAwareNetworking::class.java)
        val transportManager = mock(IntelligentTransportManager::class.java)
        val retryEngine = mock(IntelligentRetryEngine::class.java)
        val queueOptimizer = mock(QueueOptimizer::class.java)
        val routeOptimizer = mock(RouteOptimizer::class.java)

        routingEngine = RoutingEngine(
            routeManager, qosManager, congestionMonitor, routeHealthMonitor,
            topologyEngine, batteryAwareNetworking, transportManager,
            retryEngine, queueOptimizer, routeOptimizer, configManager
        )
    }

    @Test
    fun testDuplicateSuppression() {
        val packetId = "packet_123"
        
        // First time should be accepted
        assertTrue(routingEngine.markPacketProcessed(packetId))
        
        // Second time should be suppressed
        assertFalse(routingEngine.markPacketProcessed(packetId))
    }

    @Test
    fun testLoopPrevention_VisitedPath() {
        val packet = MeshPacket(
            packetId = "packet_1",
            senderId = "A",
            targetId = "C",
            payload = "test",
            ttl = 5,
            visitedPath = mutableListOf("A", "B")
        )

        // Node B is in the path, so it's a loop if B receives it again
        assertTrue(routingEngine.isRoutingLoop(packet, "B"))
        
        // Node D is not in the path, so it's valid
        assertFalse(routingEngine.isRoutingLoop(packet, "D"))
    }

    @Test
    fun testLoopPrevention_ZeroTtl() {
        val packet = MeshPacket(
            packetId = "packet_1",
            senderId = "A",
            targetId = "C",
            payload = "test",
            ttl = 0,
            visitedPath = mutableListOf("A")
        )

        // TTL is 0, should be dropped
        assertTrue(routingEngine.isRoutingLoop(packet, "D"))
    }
}
