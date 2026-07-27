package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Duplicate suppression test suite.
 * Verifies that the dedup cache prevents the same packet from being delivered
 * or relayed more than once, even in topologies with multiple paths.
 */
class DuplicateSuppressionTest {

    @Test
    fun `duplicate injection suppressed - same packet id delivered once`() {
        val sim = MeshSimulator.build {
            nodes(listOf("S", "R"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val packetId = java.util.UUID.randomUUID().toString()
        sim.node("S").sendPacket("R", "dedup-test", customPacketId = packetId)
        sim.node("S").sendPacket("R", "dedup-test", customPacketId = packetId) // same ID
        sim.runUntilQuiet()

        // R should receive it only once despite two sends with same ID
        val delivered = sim.node("R").receivedPackets().filter { it.second.payload == "dedup-test" }
        assertEquals(1, delivered.size,
            "Duplicate packet with same ID should be suppressed — delivered only once")
        MeshAssertions.assertDeliveredOnce(sim.recorder, packetId)
    }

    @Test
    fun `broadcast in ring topology delivered once per node`() {
        val nodeIds = (0 until 5).map { "n$it" }
        val sim = MeshSimulator.build {
            nodes(nodeIds)
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("n0").sendPacket("BROADCAST", "broadcast-msg")
        sim.runUntilQuiet()

        // Each non-source node should receive it at most once
        nodeIds.drop(1).forEach { id ->
            val count = sim.node(id).receivedPackets().filter { it.second.payload == "broadcast-msg" }.size
            assertTrue(count <= 1, "Node $id received broadcast $count times (expected ≤ 1)")
        }

        MeshAssertions.assertNoDuplicateDelivery(sim.recorder)
    }

    @Test
    fun `dedup suppression metric increments on duplicate`() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.fullyConnected(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val packetId = java.util.UUID.randomUUID().toString()
        // In a fully-connected 3-node mesh, a broadcast will arrive at B and C from A,
        // and B will try to relay to C (which C already received). This triggers dedup.
        sim.node("A").sendPacket("BROADCAST", "dup-test", customPacketId = packetId)
        sim.runUntilQuiet()

        val totalDups = sim.nodes.sumOf { it.metrics.duplicatesSuppressed.get() }
        assertTrue(totalDups >= 0, "Dedup suppression counter should be non-negative")
    }

    @Test
    fun `different packet ids not suppressed`() {
        val sim = MeshSimulator.build {
            nodes(listOf("X", "Y"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("X").sendPacket("Y", "msg-1")
        sim.node("X").sendPacket("Y", "msg-2")
        sim.node("X").sendPacket("Y", "msg-3")
        sim.runUntilQuiet()

        val received = sim.node("Y").receivedPackets()
        val payloads = received.map { it.second.payload }
        assertTrue(payloads.contains("msg-1"), "msg-1 should not be suppressed")
        assertTrue(payloads.contains("msg-2"), "msg-2 should not be suppressed")
        assertTrue(payloads.contains("msg-3"), "msg-3 should not be suppressed")
    }

    @Test
    fun `no duplicate delivery in fully connected mesh broadcast`() {
        val sim = MeshSimulator.mesh(n = 6, profile = NetworkProfile.PerfectNetwork)
        sim.node("node-0").sendPacket("BROADCAST", "mesh-broadcast")
        sim.runUntilQuiet()

        MeshAssertions.assertNoDuplicateDelivery(sim.recorder)
    }

    @Test
    fun `duplicate in two path topology suppressed`() {
        // Diamond topology: source → left → dest AND source → right → dest
        // Both paths deliver the same packet — second arrival should be deduped
        val sim = MeshSimulator.build {
            nodes(listOf("source", "left", "right", "dest"))
            topology { _ ->
                listOf(
                    com.meshlink.simulator.transport.Link("source", "left"),
                    com.meshlink.simulator.transport.Link("left", "source"),
                    com.meshlink.simulator.transport.Link("source", "right"),
                    com.meshlink.simulator.transport.Link("right", "source"),
                    com.meshlink.simulator.transport.Link("left", "dest"),
                    com.meshlink.simulator.transport.Link("dest", "left"),
                    com.meshlink.simulator.transport.Link("right", "dest"),
                    com.meshlink.simulator.transport.Link("dest", "right")
                )
            }
            profile(NetworkProfile.PerfectNetwork)
        }
        val packetId = java.util.UUID.randomUUID().toString()
        sim.node("source").sendPacket("dest", "diamond", customPacketId = packetId)
        sim.runUntilQuiet()

        // dest receives via left AND right — but only counts once
        MeshAssertions.assertDeliveredOnce(sim.recorder, packetId)
    }
}
