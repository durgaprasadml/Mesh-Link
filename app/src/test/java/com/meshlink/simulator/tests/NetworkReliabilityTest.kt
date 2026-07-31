package com.meshlink.simulator.tests

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteMetrics
import com.meshlink.domain.model.RouteType
import com.meshlink.recovery.engine.MeshHealthManager
import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.routing.engine.DuplicateSuppressionEngine
import com.meshlink.routing.engine.IntelligentRetryEngine
import com.meshlink.routing.engine.RouteCache
import com.meshlink.routing.engine.RouteOptimizer
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import com.meshlink.simulator.transport.Link
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetworkReliabilityTest {

    @Test
    fun `testRelayNodeFailureAndAutoRecovery`() {
        val sim = MeshSimulator.build {
            nodes(listOf("src", "relay1", "relay2", "dst"))
            topology { _ ->
                listOf(
                    Link("src", "relay1"), Link("relay1", "src"),
                    Link("src", "relay2"), Link("relay2", "src"),
                    Link("relay1", "dst"), Link("dst", "relay1"),
                    Link("relay2", "dst"), Link("dst", "relay2")
                )
            }
            profile(NetworkProfile.PerfectNetwork)
        }

        // Primary relay crashes
        sim.crash("relay1")
        sim.node("src").sendPacket("dst", "failover-payload")
        sim.runUntilQuiet()

        assertTrue(
            sim.node("dst").receivedPackets().any { it.second.payload == "failover-payload" },
            "Packet should automatically recover and route via relay2 when relay1 fails"
        )
    }

    @Test
    fun `testMeshPartitionSplitAndHealWithAntiEntropy`() {
        val group1 = listOf("n0", "n1")
        val group2 = listOf("n2", "n3")

        val sim = MeshSimulator.build {
            nodes(group1 + group2)
            topology { _ ->
                TopologyBuilder.line(group1) + TopologyBuilder.line(group2) +
                        listOf(Link("n1", "n2"), Link("n2", "n1"))
            }
            profile(NetworkProfile.PerfectNetwork)
        }

        // Split mesh
        sim.partition(group1, group2)
        sim.node("n0").sendPacket("n3", "partition-data")
        sim.step(1000)

        assertTrue(sim.node("n3").receivedPackets().isEmpty(), "No delivery across split partition")

        // Reconnect/Heal mesh
        sim.heal(group1, group2)
        sim.runUntilQuiet(maxStepMs = 5000)

        assertTrue(
            sim.node("n3").receivedPackets().any { it.second.payload == "partition-data" },
            "Packet delivered after partition heal anti-entropy sync"
        )
    }

    @Test
    fun `testCongestionBackpressureAndAdaptiveRetry`() {
        val monitor = CongestionMonitor()
        assertEquals(com.meshlink.routing.engine.CongestionLevel.LOW, monitor.congestionLevel.value)

        repeat(350) { monitor.incrementPending() }
        assertTrue(monitor.isCongested(), "Monitor should report congestion when queue exceeds threshold")

        val batteryAware = io.mockk.mockk<com.meshlink.routing.engine.BatteryAwareNetworking>(relaxed = true)
        io.mockk.every { batteryAware.powerState } returns kotlinx.coroutines.flow.MutableStateFlow(com.meshlink.routing.engine.PowerState.NORMAL)
        val retryEngine = IntelligentRetryEngine(monitor, batteryAware)

        val delayNormal = retryEngine.calculateAdaptiveRetryDelay(1, PacketPriority.NORMAL, RouteType.BLE)
        val delayCritical = retryEngine.calculateAdaptiveRetryDelay(1, PacketPriority.CRITICAL, RouteType.WIFI_DIRECT)

        assertTrue(delayCritical < delayNormal, "Critical priority on Wi-Fi Direct should retry faster than Normal on BLE")
    }

    @Test
    fun `testLoadBalancingMultiPath`() {
        val cache = RouteCache(com.meshlink.config.RuntimeConfigManager(com.meshlink.config.MeshConfig()))
        val route1 = RouteEntry(
            destinationId = "target",
            nextHop = "hopA",
            hops = 2,
            score = 90,
            metrics = RouteMetrics(rssi = -60, averageLatencyMs = 20)
        )
        val route2 = RouteEntry(
            destinationId = "target",
            nextHop = "hopB",
            hops = 2,
            score = 88,
            metrics = RouteMetrics(rssi = -62, averageLatencyMs = 22)
        )
        cache.addOrUpdateRoute(route1)
        cache.addOrUpdateRoute(route2)

        val optimizer = RouteOptimizer(cache)
        val selectedHops = mutableSetOf<String>()

        repeat(10) {
            val selected = optimizer.getLoadBalancedRoute("target")
            assertNotNull(selected)
            selectedHops.add(selected.nextHop)
        }

        assertTrue(selectedHops.contains("hopA") && selectedHops.contains("hopB"), "Load balancer should distribute traffic across both healthy hops")
    }

    @Test
    fun `testDuplicateSuppressionCompositeKey`() {
        val dedupEngine = DuplicateSuppressionEngine()
        val packet = MeshPacket(
            packetId = "p100",
            senderId = "nodeA",
            targetId = "nodeB",
            payload = "hello",
            sequenceNumber = 42L
        )

        assertFalse(dedupEngine.checkAndMark(packet), "First packet should not be duplicate")
        assertTrue(dedupEngine.checkAndMark(packet), "Second identical packet must be detected as duplicate")
    }

    @Test
    fun `testMeshHealthManagerScoreCalculation`() {
        val healthManager = MeshHealthManager()
        healthManager.updateNodeConfidence("peer1", batteryLevel = 90, lastSeenMs = System.currentTimeMillis(), isReachable = true)

        assertEquals(100, healthManager.healthMetrics.value.networkHealthScore)

        healthManager.updateMetricsSnapshot(
            meshSize = 5,
            connectedPeersCount = 4,
            congestionLevel = com.meshlink.routing.engine.CongestionLevel.HIGH,
            transports = emptyMap(),
            avgRtt = 150L,
            lossRate = 0.2f,
            retries = 5L,
            repairs = 1,
            discoveries = 2,
            queueSize = 10,
            partitionSplits = 1,
            partitionHeals = 1,
            batteryImpact = "MODERATE"
        )

        assertTrue(healthManager.healthMetrics.value.networkHealthScore < 100, "Health score should degrade on high congestion & loss")
    }

    @Test
    fun `test20NodeTopologySimulation`() {
        val nodes = (1..20).map { "node-$it" }
        val sim = MeshSimulator.build {
            nodes(nodes)
            topology { ids -> TopologyBuilder.randomMesh(ids, density = 0.4f) }
            profile(NetworkProfile.PerfectNetwork)
        }

        sim.node("node-1").sendPacket("node-20", "20-node-test")
        sim.runUntilQuiet()

        assertTrue(
            sim.node("node-20").receivedPackets().any { it.second.payload == "20-node-test" },
            "20-node mesh should successfully route and deliver packet"
        )
    }

    @Test
    fun `test50NodeTopologySimulation`() {
        val nodes = (1..50).map { "node-$it" }
        val sim = MeshSimulator.build {
            nodes(nodes)
            topology { ids -> TopologyBuilder.randomMesh(ids, density = 0.2f) }
            profile(NetworkProfile.PerfectNetwork)
        }

        sim.node("node-1").sendPacket("node-50", "50-node-cluster-test")
        sim.runUntilQuiet()

        assertTrue(
            sim.node("node-50").receivedPackets().any { it.second.payload == "50-node-cluster-test" },
            "50-node random mesh should successfully route and deliver packet"
        )
    }
}
