package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Basic two-hop and five-hop routing tests under perfect network conditions.
 * Validates the fundamental packet send → relay → deliver flow.
 */
class BasicRoutingTest {

    @Test
    fun `two node direct delivery`() {
        val sim = MeshSimulator.build {
            nodes(listOf("alice", "bob"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("alice").sendPacket("bob", "hello-bob")
        sim.runUntilQuiet()

        assertTrue(sim.node("bob").receivedPackets().isNotEmpty(),
            "Bob should have received Alice's packet")
        assertEquals("hello-bob", sim.node("bob").receivedPackets().first().second.payload)
        MeshAssertions.assertNoDeadlock(sim)
    }

    @Test
    fun `three node relay delivery`() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("A").sendPacket("C", "three-hop-test")
        sim.runUntilQuiet()

        val received = sim.node("C").receivedPackets()
        assertTrue(received.any { it.second.payload == "three-hop-test" },
            "C should have received the message relayed through B")
    }

    @Test
    fun `five node linear delivery`() {
        val sim = MeshSimulator.line(n = 5)
        sim.node("node-0").sendPacket("node-4", "end-to-end")
        sim.runUntilQuiet()

        assertTrue(sim.node("node-4").receivedPackets().isNotEmpty(),
            "node-4 should receive the message through 4 hops")
    }

    @Test
    fun `reply packet delivered back to sender`() {
        val sim = MeshSimulator.build {
            nodes(listOf("S", "R"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("S").sendPacket("R", "ping")
        sim.runUntilQuiet()

        assertTrue(sim.node("R").receivedPackets().any { it.second.payload == "ping" })
        sim.node("R").sendPacket("S", "pong")
        sim.runUntilQuiet()

        assertTrue(sim.node("S").receivedPackets().any { it.second.payload == "pong" },
            "Sender should receive the reply")
    }

    @Test
    fun `multiple packets between same nodes delivered correctly`() {
        val sim = MeshSimulator.build {
            nodes(listOf("X", "Y"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        repeat(5) { i ->
            sim.node("X").sendPacket("Y", "message-$i")
        }
        sim.runUntilQuiet()

        val received = sim.node("Y").receivedPackets()
        assertEquals(5, received.size, "All 5 messages should be received by Y")
    }

    @Test
    fun `packet not delivered if no path exists`() {
        // Two isolated nodes (no links)
        val sim = MeshSimulator.build {
            nodes(listOf("isolated-A", "isolated-B"))
            topology { emptyList() }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("isolated-A").sendPacket("isolated-B", "unreachable")
        sim.runUntilQuiet()

        assertTrue(sim.node("isolated-B").receivedPackets().isEmpty(),
            "No delivery should occur without a link")
    }

    @Test
    fun `delivery metrics increment correctly`() {
        val sim = MeshSimulator.line(n = 3)
        sim.node("node-0").sendPacket("node-2", "metric-test")
        sim.runUntilQuiet()

        val senderMetrics = sim.node("node-0").metrics
        assertTrue(senderMetrics.packetsSent.get() >= 1, "Sender should have 1+ sent packets")
    }
}
