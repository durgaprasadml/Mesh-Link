package com.meshlink.routing.engine

import com.meshlink.config.RuntimeConfigManager
import com.meshlink.domain.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HybridRoutingEngineTest {

    private val routeCache = RouteCache(mockk(relaxed = true) {
        every { currentConfig.value } returns mockk(relaxed = true) {
            every { emaAlpha } returns 0.3f
            every { routeSwitchingThreshold } returns 5
        }
    })
    private val routeScorer = RouteScorer()
    private val routeOptimizer = RouteOptimizer(routeCache)
    private val routeManager = RouteManager(routeCache, routeScorer, routeOptimizer)

    private val qosManager = mockk<QoSManager>(relaxed = true)
    private val congestionMonitor = mockk<CongestionMonitor>(relaxed = true)
    private val routeHealthMonitor = mockk<RouteHealthMonitor>(relaxed = true)
    private val topologyEngine = mockk<NetworkTopologyEngine>(relaxed = true)
    private val batteryAwareNetworking = mockk<BatteryAwareNetworking>(relaxed = true)
    private val transportManager = mockk<IntelligentTransportManager>(relaxed = true)
    private val retryEngine = mockk<IntelligentRetryEngine>(relaxed = true)
    private val queueOptimizer = mockk<QueueOptimizer>(relaxed = true)
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
            routeOptimizer = routeOptimizer,
            configManager = configManager
        )
    }

    @After
    fun tearDown() {
        routeCache.clear()
        clearAllMocks()
    }

    @Test
    fun `test hybrid route registration and scoring for heterogeneous path`() {
        // Node A -> B (BLE) -> C (Wi-Fi Direct) -> D (BLE)
        routeManager.updateRoute(
            destinationId = "Node_D",
            nextHop = "Node_B",
            hops = 3,
            rssi = -60,
            trustScore = 90,
            type = RouteType.BLE
        )

        val optimalRoute = routeOptimizer.getOptimalRoute("Node_D")
        assertNotNull(optimalRoute)
        assertEquals("Node_D", optimalRoute?.destinationId)
        assertEquals("Node_B", optimalRoute?.nextHop)
        assertEquals(3, optimalRoute?.hops)
        assertTrue(optimalRoute!!.score > 0)
    }

    @Test
    fun `test route scorer favors Wi-Fi Direct for media chunk payloads`() {
        val bleRoute = RouteEntry(
            destinationId = "TargetNode",
            nextHop = "BlePeer",
            hops = 1,
            routeType = RouteType.BLE,
            currentTransport = RouteType.BLE
        )

        val wifiRoute = RouteEntry(
            destinationId = "TargetNode",
            nextHop = "WifiPeer",
            hops = 1,
            routeType = RouteType.WIFI_DIRECT,
            currentTransport = RouteType.WIFI_DIRECT
        )

        val bleScoreText = routeScorer.calculateScore(bleRoute, PacketType.TEXT)
        val wifiScoreText = routeScorer.calculateScore(wifiRoute, PacketType.TEXT)

        val bleScoreMedia = routeScorer.calculateScore(bleRoute, PacketType.MEDIA_CHUNK)
        val wifiScoreMedia = routeScorer.calculateScore(wifiRoute, PacketType.MEDIA_CHUNK)

        assertTrue("Wi-Fi Direct score for media should be significantly higher than BLE", wifiScoreMedia > bleScoreMedia)
        assertTrue("BLE score for text should be higher than or competitive with Wi-Fi", bleScoreText >= wifiScoreText || (wifiScoreText - bleScoreText) < 5)
    }

    @Test
    fun `test secondary backup route lookup on primary hop failure`() {
        routeManager.updateRoute("Node_C", "PrimaryHop", hops = 1, rssi = -50, type = RouteType.WIFI_DIRECT)
        routeManager.updateRoute("Node_C", "BackupHop", hops = 2, rssi = -65, type = RouteType.BLE)

        val primaryRoute = routeOptimizer.getOptimalRoute("Node_C")
        assertEquals("PrimaryHop", primaryRoute?.nextHop)

        val backupRoutes = routeOptimizer.getBackupRoutes("Node_C", primaryNextHop = "PrimaryHop")
        assertEquals(1, backupRoutes.size)
        assertEquals("BackupHop", backupRoutes.first().nextHop)
    }

    @Test
    fun `test duplicate suppression prevents repeated processing`() {
        val packetId = "unique_pkt_999"
        val isFirstTime = routingEngine.markPacketProcessed(packetId)
        val isSecondTime = routingEngine.markPacketProcessed(packetId)

        assertTrue(isFirstTime)
        assertFalse(isSecondTime)
    }

    @Test
    fun `test loop prevention detects visited path and zero TTL`() {
        val loopPacket = MeshPacket(
            packetId = "pkt_loop_test",
            senderId = "Node_A",
            targetId = "Node_D",
            payload = "Hello",
            ttl = 4,
            visitedPath = listOf("Node_A", "Node_B", "MyLocalNode")
        )

        val isLoop = routingEngine.isRoutingLoop(loopPacket, localMeshId = "MyLocalNode")
        assertTrue("Packet with local node in visited path must be flagged as a routing loop", isLoop)

        val expiredPacket = MeshPacket(
            packetId = "pkt_expired_test",
            senderId = "Node_A",
            targetId = "Node_D",
            payload = "Hello",
            ttl = 0,
            visitedPath = listOf("Node_A")
        )

        val isExpiredLoop = routingEngine.isRoutingLoop(expiredPacket, localMeshId = "MyLocalNode")
        assertTrue("Packet with TTL <= 0 must be flagged as loop/drop condition", isExpiredLoop)
    }

    @Test
    fun `test dynamic initial TTL calculation scales with mesh size`() {
        // Empty route cache
        val smallTtl = routeOptimizer.calculateDynamicTtl()
        assertTrue(smallTtl <= 4)

        // Add multiple destinations
        for (i in 1..30) {
            routeManager.updateRoute("Dest_$i", "Hop_$i", hops = 1)
        }

        val largerTtl = routeOptimizer.calculateDynamicTtl()
        assertTrue("TTL should expand as mesh network grows", largerTtl > smallTtl)
    }

    @Test
    fun `test route eviction upon peer disconnection`() {
        routeManager.updateRoute("Dest_1", "DisconnectingPeer", hops = 1)
        routeManager.updateRoute("Dest_2", "DisconnectingPeer", hops = 2)
        routeManager.updateRoute("Dest_3", "StablePeer", hops = 1)

        assertEquals(3, routeCache.getAllDestinations().size)

        routeManager.handlePeerDisconnected("DisconnectingPeer")

        val remainingDestinations = routeCache.getAllDestinations()
        assertEquals(1, remainingDestinations.size)
        assertEquals("Dest_3", remainingDestinations.first())
    }
}
