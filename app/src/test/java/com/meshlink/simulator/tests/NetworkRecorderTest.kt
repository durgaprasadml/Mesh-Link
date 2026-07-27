package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.metrics.NetworkRecorder
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * NetworkRecorder test suite.
 * Validates that the packet event recording infrastructure (Req 3) correctly
 * captures, correlates, and exports all packet lifecycle events.
 */
class NetworkRecorderTest {

    @Test
    fun `events recorded in chronological order`() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("A").sendPacket("C", "order-check")
        sim.runUntilQuiet()

        val events = sim.recorder.allEvents()
        assertTrue(events.isNotEmpty(), "At least one event should be recorded")

        // All events should be in non-decreasing virtual-time order
        val times = events.map { it.virtualTimeMs }
        for (i in 1 until times.size) {
            assertTrue(times[i] >= times[i - 1],
                "Events at index $i (t=${times[i]}) should not precede index ${i-1} (t=${times[i-1]})")
        }
    }

    @Test
    fun `trace id tracks all hops from source to destination`() {
        val sim = MeshSimulator.build {
            nodes(listOf("src", "hop1", "hop2", "dst"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("src").sendPacket("dst", "trace-me")
        sim.runUntilQuiet()

        val trace = sim.recorder.getTraceForPacket(traceId)
        assertTrue(trace.isNotEmpty(),
            "Trace for traceId '$traceId' should contain events")

        // Should have at least a SENT and a DELIVERED event
        assertTrue(trace.any { it.eventType == NetworkRecorder.EventType.SENT },
            "Trace should contain a SENT event")
        assertTrue(trace.any { it.eventType == NetworkRecorder.EventType.DELIVERED },
            "Trace should contain a DELIVERED event")
    }

    @Test
    fun `dropped packets reported with reason`() {
        val sim = MeshSimulator.build {
            nodes(listOf("X", "Y"))
            topology { _ -> emptyList() }  // No links → NO_ROUTE drop
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("X").sendPacket("Y", "will-drop")
        sim.runUntilQuiet()

        val drops = sim.recorder.getDroppedPackets()
        assertTrue(drops.isNotEmpty(), "At least one drop event should be recorded")
        assertNotNull(drops.first().dropReason, "Drop reason should not be null")
    }

    @Test
    fun `json export is valid and contains correct event count`() {
        val sim = MeshSimulator.build {
            nodes(listOf("P", "Q"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("P").sendPacket("Q", "json-export")
        sim.runUntilQuiet()

        val json = sim.recorder.exportToJson()
        assertTrue(json.startsWith("["), "JSON export should start with [")
        assertTrue(json.endsWith("]"), "JSON export should end with ]")

        // Count events by counting "\"type\":" occurrences
        val eventCount = json.split("\"type\":").size - 1
        assertEquals(sim.recorder.eventCount(), eventCount,
            "JSON export should contain ${sim.recorder.eventCount()} events")
    }

    @Test
    fun `recorder reset clears all events between tests`() {
        val sim = MeshSimulator.build {
            nodes(listOf("R1", "R2"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("R1").sendPacket("R2", "before-reset")
        sim.runUntilQuiet()
        assertTrue(sim.recorder.eventCount() > 0, "Should have events before reset")

        sim.recorder.reset()
        assertEquals(0, sim.recorder.eventCount(), "After reset, event count should be 0")

        val events = sim.recorder.allEvents()
        assertTrue(events.isEmpty(), "After reset, allEvents() should return empty list")
    }

    @Test
    fun `was delivered to returns correct result`() {
        val sim = MeshSimulator.build {
            nodes(listOf("source", "target"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val packetId = java.util.UUID.randomUUID().toString()
        sim.node("source").sendPacket("target", "delivery-check", customPacketId = packetId)
        sim.runUntilQuiet()

        assertTrue(sim.recorder.wasDeliveredTo(packetId, "target"),
            "wasDeliveredTo should return true for delivered packet")
        assertTrue(!sim.recorder.wasDeliveredTo(packetId, "nonexistent"),
            "wasDeliveredTo should return false for node that never received the packet")
    }

    @Test
    fun `delivery count is 1 for non-duplicate packet`() {
        val sim = MeshSimulator.build {
            nodes(listOf("S", "D"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val packetId = java.util.UUID.randomUUID().toString()
        sim.node("S").sendPacket("D", "unique", customPacketId = packetId)
        sim.runUntilQuiet()

        assertEquals(1, sim.recorder.deliveryCount(packetId),
            "Non-duplicate packet should be delivered exactly once")
        MeshAssertions.assertDeliveredOnce(sim.recorder, packetId)
    }

    @Test
    fun `print trace executes without exception`() {
        val sim = MeshSimulator.build {
            nodes(listOf("T1", "T2"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("T1").sendPacket("T2", "trace-print")
        sim.runUntilQuiet()

        // Should not throw — output goes to stdout which is captured in test
        sim.recorder.printTrace(traceId)
    }

    @Test
    fun `visited nodes reflects all relay hops`() {
        val sim = MeshSimulator.build {
            nodes(listOf("n0", "n1", "n2", "n3"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val traceId = sim.node("n0").sendPacket("n3", "hop-trace")
        sim.runUntilQuiet()

        val packetId = sim.recorder.getTraceForPacket(traceId)
            .firstOrNull()?.packetId ?: return

        val visited = sim.recorder.visitedNodes(packetId)
        assertTrue(visited.isNotEmpty(), "Visited nodes should not be empty for delivered packet")
        assertTrue(visited.contains("n3"), "Final destination n3 should be in visited nodes")
    }

    @Test
    fun `events for packet returns subset for specific packet id`() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val id1 = java.util.UUID.randomUUID().toString()
        val id2 = java.util.UUID.randomUUID().toString()

        sim.node("A").sendPacket("B", "p1", customPacketId = id1)
        sim.node("A").sendPacket("B", "p2", customPacketId = id2)
        sim.runUntilQuiet()

        val eventsForP1 = sim.recorder.eventsForPacket(id1)
        val eventsForP2 = sim.recorder.eventsForPacket(id2)

        assertTrue(eventsForP1.all { it.packetId == id1 },
            "eventsForPacket(id1) should only contain events for id1")
        assertTrue(eventsForP2.all { it.packetId == id2 },
            "eventsForPacket(id2) should only contain events for id2")
        assertTrue(eventsForP1.none { it.packetId == id2 },
            "eventsForPacket(id1) should not contain id2 events")
    }
}
