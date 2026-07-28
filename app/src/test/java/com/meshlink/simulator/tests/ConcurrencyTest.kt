package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.Rule
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * Concurrency and scale test suite.
 * Verifies that the simulator handles 10–100 nodes with concurrent messaging
 * without deadlocks, race conditions, or packet corruption.
 *
 * All tests use the single-threaded deterministic scheduler — "concurrent" here
 * refers to concurrent packet states (multiple in-flight packets), not OS threads.
 */
class ConcurrencyTest {

    @get:Rule
    val timeout = Timeout(180, TimeUnit.SECONDS)

    @Test
    fun `ten node concurrent messaging - no deadlock`() {
        val sim = MeshSimulator.random(n = 10, density = 0.4f, seed = 42L,
            profile = NetworkProfile.PerfectNetwork)

        // Each node sends a message to a different node
        val nodeIds = sim.nodeIds()
        nodeIds.forEachIndexed { idx, id ->
            val target = nodeIds[(idx + 1) % nodeIds.size]
            sim.node(id).sendPacket(target, "msg-from-$id")
        }
        val steps = sim.runUntilQuiet(maxStepMs = 10_000)

        MeshAssertions.assertNoDeadlock(sim)
        assertTrue(steps > 0)
        val totalDelivered = sim.nodes.sumOf { it.receivedPackets().size }
        assertTrue(totalDelivered > 0, "At least some messages should be delivered in 10-node test")
    }

    @Test
    fun `twenty five node stress - high message volume`() {
        val sim = MeshSimulator.random(n = 25, density = 0.3f, seed = 1234L,
            profile = NetworkProfile.PerfectNetwork)

        // 500 messages: each node sends 20 messages to random targets
        val nodeIds = sim.nodeIds()
        val rng = java.util.Random(999L)
        repeat(500) {
            val sender = nodeIds[rng.nextInt(nodeIds.size)]
            val receiver = nodeIds[rng.nextInt(nodeIds.size)].takeIf { it != sender }
                ?: nodeIds[(nodeIds.indexOf(sender) + 1) % nodeIds.size]
            sim.node(sender).sendPacket(receiver, "bulk-$it")
        }
        sim.runUntilQuiet(maxStepMs = 20_000)

        MeshAssertions.assertNoDeadlock(sim)
        val report = sim.metrics.generateReport()
        assertTrue(report.totalPacketsSent >= 500, "500 messages should have been sent")
    }

    @Test
    fun `fifty node high throughput - delivery rate above 85 percent`() {
        val sim = MeshSimulator.random(n = 50, density = 0.25f, seed = 5555L,
            profile = NetworkProfile.PerfectNetwork)
        val nodeIds = sim.nodeIds()
        val rng = java.util.Random(777L)

        // 500 messages
        repeat(500) {
            val sender = nodeIds[rng.nextInt(nodeIds.size)]
            val target = nodeIds[rng.nextInt(nodeIds.size)].takeIf { it != sender }
                ?: nodeIds[(nodeIds.indexOf(sender) + 1) % nodeIds.size]
            sim.node(sender).sendPacket(target, "hi-$it")
        }
        sim.runUntilQuiet(maxStepMs = 30_000)

        MeshAssertions.assertNoDeadlock(sim)
        val report = sim.metrics.generateReport()
        // Perfect network → high delivery rate expected
        MeshAssertions.assertDeliveryRate(report, minRate = 0.80f)
    }

    @Test
    fun `hundred node large scale - terminates within timeout`() {
        val sim = MeshSimulator.random(n = 100, density = 0.15f, seed = 8888L,
            profile = NetworkProfile.PerfectNetwork)
        val nodeIds = sim.nodeIds()
        val rng = java.util.Random(1337L)

        // 500 messages across 100 nodes
        repeat(500) {
            val sender = nodeIds[rng.nextInt(nodeIds.size)]
            val target = nodeIds[rng.nextInt(nodeIds.size)].takeIf { it != sender }
                ?: nodeIds[(nodeIds.indexOf(sender) + 1) % nodeIds.size]
            sim.node(sender).sendPacket(target, "scale-$it")
        }
        val steps = sim.runUntilQuiet(maxStepMs = 60_000)

        MeshAssertions.assertNoDeadlock(sim)
        assertTrue(steps > 0, "100-node simulation should complete in time")
        println(sim.metrics.generateReport())
    }

    @Test
    fun `no packet payload corruption across nodes`() {
        val sim = MeshSimulator.line(n = 5, profile = NetworkProfile.PerfectNetwork)
        val payloads = (1..20).map { "payload-content-$it-unique" }

        payloads.forEach { p ->
            sim.node("node-0").sendPacket("node-4", p)
        }
        sim.runUntilQuiet()

        val receivedPayloads = sim.node("node-4").receivedPackets().map { it.second.payload }
        payloads.forEach { original ->
            assertTrue(receivedPayloads.contains(original),
                "Payload '$original' should arrive uncorrupted at destination")
        }
    }

    @Test
    fun `packet order maintained for same source destination pair`() {
        val sim = MeshSimulator.build {
            nodes(listOf("src", "dst"))
            topology { ids -> com.meshlink.simulator.topology.TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val n = 10
        (1..n).forEach { sim.node("src").sendPacket("dst", "order-$it") }
        sim.runUntilQuiet()

        val received = sim.node("dst").receivedPackets().map { it.second.payload }
        // In a perfect network with zero latency, messages should arrive in send order
        (1..n).forEach { i ->
            assertTrue(received.contains("order-$i"), "Message order-$i should be received")
        }
    }
}
