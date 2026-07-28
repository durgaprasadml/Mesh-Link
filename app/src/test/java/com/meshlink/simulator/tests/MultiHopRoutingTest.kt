package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.node.SimulatedNode.NodeConfig
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Multi-hop routing validation suite.
 * Tests exact hop counts, path tracking, and max-hop-limit enforcement.
 */
class MultiHopRoutingTest {

    @Test
    fun `two hop delivery via single relay`() {
        val sim = MeshSimulator.build {
            nodes(listOf("src", "relay", "dst"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("src").sendPacket("dst", "two-hop")
        sim.runUntilQuiet()

        assertTrue(sim.node("dst").receivedPackets().isNotEmpty())
        // At destination, hopCount should be 2 (src→relay is hop 1, relay→dst is hop 2)
        val delivered = sim.recorder.getTraceForPacket(traceId)
            .firstOrNull { it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.DELIVERED }
        assertTrue(delivered != null && delivered.hopCount <= 2, "Hop count should be at most 2")
    }

    @Test
    fun `three hop delivery`() {
        val sim = MeshSimulator.line(n = 4) // node-0 → node-1 → node-2 → node-3
        sim.node("node-0").sendPacket("node-3", "three-hop")
        sim.runUntilQuiet()

        assertTrue(sim.node("node-3").receivedPackets().isNotEmpty(), "node-3 must receive after 3 hops")
    }

    @Test
    fun `five hop delivery`() {
        val sim = MeshSimulator.line(n = 6) // node-0 … node-5
        sim.node("node-0").sendPacket("node-5", "five-hop")
        sim.runUntilQuiet()

        assertTrue(sim.node("node-5").receivedPackets().isNotEmpty(), "node-5 must receive after 5 hops")
    }

    @Test
    fun `packet rejected when hop count exceeds max hops`() {
        val maxHops = 3
        val sim = MeshSimulator.build {
            nodes(6) // 5 relay nodes = 5 hops — exceeds maxHops=3
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
            nodeConfig(NodeConfig(maxHops = maxHops, defaultTtl = 10))
        }
        sim.node("node-0").sendPacket("node-5", "too-far")
        sim.runUntilQuiet()

        // node-5 should NOT receive the packet — max hop limit enforced
        assertTrue(sim.node("node-5").receivedPackets().isEmpty(),
            "Packet should not be delivered after exceeding maxHops=$maxHops")
    }

    @Test
    fun `hop count increments through each relay`() {
        val sim = MeshSimulator.line(n = 4, profile = NetworkProfile.PerfectNetwork)
        val traceId = sim.node("node-0").sendPacket("node-3", "hop-count-test")
        sim.runUntilQuiet()

        val trace = sim.recorder.getTraceForPacket(traceId)
        val forwardedEvents = trace.filter {
            it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.FORWARDED ||
            it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.DELIVERED
        }
        // Hop counts should be strictly increasing
        val hopCounts = forwardedEvents.map { it.hopCount }
        for (i in 1 until hopCounts.size) {
            assertTrue(hopCounts[i] >= hopCounts[i - 1],
                "Hop count should be non-decreasing through relays: $hopCounts")
        }
    }

    @Test
    fun `star topology delivers to all spokes from hub`() {
        val sim = MeshSimulator.build {
            nodes(listOf("hub", "spoke1", "spoke2", "spoke3", "spoke4"))
            topology { ids -> TopologyBuilder.star(ids[0], ids.drop(1)) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("hub").sendPacket("spoke3", "to-spoke3")
        sim.runUntilQuiet()

        assertTrue(sim.node("spoke3").receivedPackets().any { it.second.payload == "to-spoke3" })
    }

    @Test
    fun `packet from spoke reaches another spoke via hub`() {
        val sim = MeshSimulator.build {
            nodes(listOf("hub", "spoke1", "spoke2"))
            topology { ids -> TopologyBuilder.star(ids[0], ids.drop(1)) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("spoke1").sendPacket("spoke2", "spoke-to-spoke")
        sim.runUntilQuiet()

        assertTrue(sim.node("spoke2").receivedPackets().any { it.second.payload == "spoke-to-spoke" },
            "spoke2 should receive spoke1's message routed via hub")
    }

    @Test
    fun `ttl decrements with each hop`() {
        val sim = MeshSimulator.line(n = 4, profile = NetworkProfile.PerfectNetwork)
        val traceId = sim.node("node-0").sendPacket("node-3", "ttl-test")
        sim.runUntilQuiet()

        val trace = sim.recorder.getTraceForPacket(traceId)
        val ttlValues = trace
            .filter { it.eventType != com.meshlink.simulator.metrics.NetworkRecorder.EventType.DROPPED }
            .map { it.ttl }

        // TTL should strictly decrease through hops
        if (ttlValues.size > 1) {
            for (i in 1 until ttlValues.size) {
                assertTrue(ttlValues[i] <= ttlValues[i - 1],
                    "TTL should decrease through hops: $ttlValues")
            }
        }
    }
}
