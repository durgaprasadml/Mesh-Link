package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import com.meshlink.simulator.transport.Link
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Route recovery test suite.
 * Validates that the mesh finds alternate paths when primary links or nodes fail,
 * reconverges after recovery, and handles dynamic topology changes.
 */
class RouteRecoveryTest {

    @Test
    fun `link failure triggers fallback via alternate path`() {
        // Diamond: S → left → D AND S → right → D
        val sim = MeshSimulator.build {
            nodes(listOf("S", "left", "right", "D"))
            topology { _ ->
                listOf(
                    Link("S", "left"), Link("left", "S"),
                    Link("S", "right"), Link("right", "S"),
                    Link("left", "D"), Link("D", "left"),
                    Link("right", "D"), Link("D", "right")
                )
            }
            profile(NetworkProfile.PerfectNetwork)
        }
        // Disable the left path
        sim.disableLink("S", "left")
        sim.disableLink("left", "D")

        sim.node("S").sendPacket("D", "fallback-test")
        sim.runUntilQuiet()

        assertTrue(sim.node("D").receivedPackets().any { it.second.payload == "fallback-test" },
            "Packet should be delivered via the right path after left is disabled")
    }

    @Test
    fun `node failure reroutes packets`() {
        val sim = MeshSimulator.build {
            nodes(listOf("src", "primary", "alternate", "dst"))
            topology { _ ->
                listOf(
                    Link("src", "primary"), Link("primary", "src"),
                    Link("src", "alternate"), Link("alternate", "src"),
                    Link("primary", "dst"), Link("dst", "primary"),
                    Link("alternate", "dst"), Link("dst", "alternate")
                )
            }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.crash("primary") // Primary relay is gone

        sim.node("src").sendPacket("dst", "rerouted")
        sim.runUntilQuiet()

        assertTrue(sim.node("dst").receivedPackets().any { it.second.payload == "rerouted" },
            "Packet should reach dst via alternate relay after primary crashes")
    }

    @Test
    fun `reconverges after downed node recovers`() {
        val sim = MeshSimulator.line(n = 4, profile = NetworkProfile.PerfectNetwork)

        // Take intermediate node offline
        sim.goOffline("node-2")
        sim.node("node-0").sendPacket("node-3", "recovery-test")
        sim.step(500)

        val receivedBefore = sim.node("node-3").receivedPackets()

        // Bring it back online
        sim.comeOnline("node-2")
        sim.runUntilQuiet()

        // After recovery the packet should reach node-3
        val receivedAfter = sim.node("node-3").receivedPackets()
        assertTrue(receivedAfter.any { it.second.payload == "recovery-test" },
            "After node-2 recovery, packet should be delivered to node-3")
    }

    @Test
    fun `dynamic link addition enables new delivery path`() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B"))
            topology { emptyList() } // Start with no links
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("A").sendPacket("B", "before-link")
        sim.step(100)
        assertTrue(sim.node("B").receivedPackets().isEmpty(), "No delivery before link added")

        // Dynamically add a link
        sim.addLink("A", "B")
        sim.node("A").sendPacket("B", "after-link")
        sim.runUntilQuiet()

        assertTrue(sim.node("B").receivedPackets().any { it.second.payload == "after-link" },
            "Packet should be delivered after dynamic link addition")
    }

    @Test
    fun `dynamic link removal isolates nodes`() {
        val sim = MeshSimulator.build {
            nodes(listOf("X", "Y"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        sim.node("X").sendPacket("Y", "before-removal")
        sim.runUntilQuiet()
        assertTrue(sim.node("Y").receivedPackets().isNotEmpty(), "First packet delivered OK")

        // Remove the link
        sim.removeLink("X", "Y")
        sim.node("X").sendPacket("Y", "after-removal")
        sim.runUntilQuiet()

        assertTrue(sim.node("Y").receivedPackets().none { it.second.payload == "after-removal" },
            "No delivery after link removal")
    }

    @Test
    fun `partition and heal delivers pending packets`() {
        val groupA = listOf("a0", "a1")
        val groupB = listOf("b0", "b1")
        val sim = MeshSimulator.build {
            nodes(groupA + groupB)
            topology { _ ->
                TopologyBuilder.line(groupA) + TopologyBuilder.line(groupB) +
                listOf(Link("a1", "b0"), Link("b0", "a1"))
            }
            profile(NetworkProfile.PerfectNetwork)
        }
        // Partition the two groups
        sim.partition(groupA, groupB)
        val traceId = sim.node("a0").sendPacket("b1", "cross-partition")
        sim.step(1_000)

        assertTrue(sim.node("b1").receivedPackets().isEmpty(), "No delivery during partition")

        // Heal the partition
        sim.heal(groupA, groupB)
        sim.runUntilQuiet(maxStepMs = 5_000)

        assertTrue(sim.node("b1").receivedPackets().any { it.second.payload == "cross-partition" },
            "Packet should be delivered after partition heals (via S&F if relay queued it)")
    }

    @Test
    fun `route cache updates on topology change`() {
        val sim = MeshSimulator.build {
            nodes(listOf("src", "r1", "r2", "dst"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        // Initial delivery to populate route cache
        sim.node("src").sendPacket("dst", "cache-populate")
        sim.runUntilQuiet()

        // Remove r1, add r2 direct link to src
        sim.removeLink("src", "r1")
        sim.addLink("src", "r2")

        // New packet should use updated routes
        sim.node("src").sendPacket("dst", "cache-updated")
        sim.runUntilQuiet()

        assertTrue(sim.node("dst").receivedPackets().any { it.second.payload == "cache-updated" },
            "Route cache should adapt after topology change")
    }
}
