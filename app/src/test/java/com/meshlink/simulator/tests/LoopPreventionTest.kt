package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.node.SimulatedNode.NodeConfig
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Loop prevention test suite.
 * Verifies that TTL expiry and visitedPath tracking prevent infinite packet
 * circulation in topologies with cycles.
 */
class LoopPreventionTest {

    @Test
    fun `ttl expiry terminates forwarding after one hop`() {
        // TTL=1 → only one forwarding step allowed
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C", "D"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
            nodeConfig(NodeConfig(defaultTtl = 1, maxHops = 10))
        }
        val traceId = sim.node("A").sendPacket("D", "ttl-1")
        sim.runUntilQuiet()

        // With TTL=1, packet cannot reach D (3 hops away)
        assertTrue(sim.node("D").receivedPackets().isEmpty(),
            "Packet with TTL=1 should not reach a node 3 hops away")

        MeshAssertions.assertTtlExpiredBeforeLoop(sim.recorder, traceId)
    }

    @Test
    fun `ring topology does not cause infinite loop`() {
        val nodeIds = (0 until 6).map { "ring-$it" }
        val sim = MeshSimulator.build {
            nodes(nodeIds)
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("ring-0").sendPacket("ring-3", "ring-test")
        sim.runUntilQuiet()

        // Verify no loop via visitedPath tracking
        MeshAssertions.assertNoLoop(sim.recorder, traceId)
    }

    @Test
    fun `fully connected topology has no infinite loop`() {
        val sim = MeshSimulator.mesh(n = 5, profile = NetworkProfile.PerfectNetwork)
        val traceId = sim.node("node-0").sendPacket("node-4", "fc-test")
        sim.runUntilQuiet()

        MeshAssertions.assertNoLoop(sim.recorder, traceId)
    }

    @Test
    fun `broadcast in ring visited each node at most once`() {
        val nodeIds = (0 until 5).map { "r$it" }
        val sim = MeshSimulator.build {
            nodes(nodeIds)
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("r0").sendPacket("BROADCAST", "ring-broadcast")
        sim.runUntilQuiet()

        MeshAssertions.assertNoLoop(sim.recorder, traceId)
        MeshAssertions.assertNoDuplicateDelivery(sim.recorder)
    }

    @Test
    fun `ttl count decrements and eventually stops relay`() {
        // 10 nodes in a ring with TTL=4 → packets stop after 4 hops
        val nodeIds = (0 until 10).map { "nt$it" }
        val sim = MeshSimulator.build {
            nodes(nodeIds)
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
            nodeConfig(NodeConfig(defaultTtl = 2))
        }
        sim.node("nt0").sendPacket("nt5", "ttl-bounded")
        sim.runUntilQuiet()

        // With TTL=2 and 5 hops needed, the packet should NOT reach nt5
        val received = sim.node("nt5").receivedPackets()
        if (received.isNotEmpty()) {
            println("Packet reached nt5! Path: ${received.first().second.visitedPath}, TTL: ${received.first().second.ttl}, Hops: ${received.first().second.hopCount}")
        }
        assertTrue(received.isEmpty(),
            "Packet with TTL=2 should not reach node 5 hops away in a ring")

        // TTL expirations should be recorded
        val totalExpired = sim.nodes.sumOf { it.metrics.ttlExpirations.get() }
        assertTrue(totalExpired > 0, "TTL expiration events should be recorded")
    }

    @Test
    fun `no packet forwarded to node already in visited path`() {
        // Triangle: A ↔ B ↔ C ↔ A
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("A").sendPacket("C", "triangle-test")
        sim.runUntilQuiet()

        // A should not re-receive its own forwarded packet (loop via C→B→A blocked)
        MeshAssertions.assertNoLoop(sim.recorder, traceId)
    }

    @Test
    fun `high TTL in large ring still terminates`() {
        val nodeIds = (0 until 20).map { "big$it" }
        val sim = MeshSimulator.build {
            nodes(nodeIds)
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
            nodeConfig(NodeConfig(defaultTtl = 10, maxHops = 15))
        }
        sim.node("big0").sendPacket("BROADCAST", "big-ring-broadcast")
        val steps = sim.runUntilQuiet(maxStepMs = 15_000)

        // Verify simulation terminates (runUntilQuiet returned)
        assertTrue(steps > 0, "Simulation should have taken at least 1 step")
        MeshAssertions.assertNoDeadlock(sim)
    }
}
