package com.meshlink.simulator.tests

import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import com.meshlink.simulator.topology.TopologyExporter
import com.meshlink.simulator.transport.Link
import com.meshlink.simulator.transport.TransportConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TopologyExporter test suite.
 * Validates GraphViz DOT, Mermaid, and JSON export formats (Req 5).
 */
class TopologyExporterTest {

    private val exporter = TopologyExporter()

    // ── DOT Export Tests ──────────────────────────────────────────────────────────

    @Test
    fun `dot export contains all node ids`() {
        val nodeIds = listOf("alpha", "beta", "gamma")
        val links = TopologyBuilder.ring(nodeIds)
        val dot = exporter.exportDot(nodeIds, links)

        nodeIds.forEach { id ->
            assertTrue(dot.contains("\"$id\""),
                "DOT output should contain node '$id'")
        }
    }

    @Test
    fun `dot export starts with digraph keyword`() {
        val nodeIds = listOf("n0", "n1")
        val links = TopologyBuilder.line(nodeIds)
        val dot = exporter.exportDot(nodeIds, links)

        assertTrue(dot.trimStart().startsWith("digraph"),
            "DOT output should start with 'digraph'")
    }

    @Test
    fun `dot export contains all edges`() {
        val nodeIds = listOf("A", "B", "C")
        val links = TopologyBuilder.line(nodeIds)
        val dot = exporter.exportDot(nodeIds, links)

        assertTrue(dot.contains("\"A\" -> \"B\""),
            "DOT should contain A→B edge")
        assertTrue(dot.contains("\"B\" -> \"C\""),
            "DOT should contain B→C edge")
    }

    @Test
    fun `dot export marks disabled links as dashed red`() {
        val link = Link("X", "Y", config = TransportConfig.TypicalBle)
        link.disable()
        val dot = exporter.exportDot(listOf("X", "Y"), listOf(link))

        assertTrue(dot.contains("dashed"), "Disabled link should use dashed style")
        assertTrue(dot.contains("red"), "Disabled link should be colored red")
        assertTrue(dot.contains("DISABLED"), "Disabled link label should say DISABLED")
    }

    @Test
    fun `dot export uses custom graph name`() {
        val dot = exporter.exportDot(listOf("n0"), emptyList(), graphName = "TestGraph")
        assertTrue(dot.contains("digraph TestGraph"), "Custom graph name should appear in DOT output")
    }

    // ── Mermaid Export Tests ──────────────────────────────────────────────────────

    @Test
    fun `mermaid export starts with graph LR`() {
        val nodeIds = listOf("x0", "x1")
        val links = TopologyBuilder.line(nodeIds)
        val mermaid = exporter.exportMermaid(nodeIds, links)

        assertTrue(mermaid.trimStart().startsWith("graph LR"),
            "Mermaid output should start with 'graph LR'")
    }

    @Test
    fun `mermaid export contains all node declarations`() {
        val nodeIds = listOf("alice", "bob", "carol")
        val links = TopologyBuilder.line(nodeIds)
        val mermaid = exporter.exportMermaid(nodeIds, links)

        nodeIds.forEach { id ->
            assertTrue(mermaid.contains(id),
                "Mermaid output should contain node '$id'")
        }
    }

    @Test
    fun `mermaid export contains edge arrows`() {
        val nodeIds = listOf("M0", "M1", "M2")
        val links = TopologyBuilder.line(nodeIds)
        val mermaid = exporter.exportMermaid(nodeIds, links)

        assertTrue(mermaid.contains("-->") || mermaid.contains("-..->"),
            "Mermaid output should contain edge arrows")
    }

    @Test
    fun `mermaid export uses dashed arrows for disabled links`() {
        val link = Link("D0", "D1", config = TransportConfig.TypicalBle)
        link.disable()
        val mermaid = exporter.exportMermaid(listOf("D0", "D1"), listOf(link))

        assertTrue(mermaid.contains("-.-") || mermaid.contains("DISABLED"),
            "Disabled link should use dashed arrow or DISABLED label in Mermaid")
    }

    // ── JSON Export Tests ─────────────────────────────────────────────────────────

    @Test
    fun `json export is valid json structure`() {
        val nodeIds = listOf("j0", "j1", "j2")
        val links = TopologyBuilder.ring(nodeIds)
        val json = exporter.exportJson(nodeIds, links)

        assertTrue(json.contains("\"nodes\""), "JSON should contain nodes array")
        assertTrue(json.contains("\"links\""), "JSON should contain links array")
        assertTrue(json.trimStart().startsWith("{"), "JSON should start with {")
        assertTrue(json.trimEnd().endsWith("}"), "JSON should end with }")
    }

    @Test
    fun `json export node count matches simulation`() {
        val nodeIds = listOf("a", "b", "c", "d", "e")
        val links = TopologyBuilder.ring(nodeIds)
        val json = exporter.exportJson(nodeIds, links)

        val nodeCount = json.split("\"id\":").size - 1
        assertEquals(nodeIds.size, nodeCount,
            "JSON should contain ${nodeIds.size} node entries")
    }

    @Test
    fun `json export link count matches topology`() {
        val nodeIds = listOf("n0", "n1", "n2")
        // Line: 2 bidirectional links = 4 directed links
        val links = TopologyBuilder.line(nodeIds)
        val json = exporter.exportJson(nodeIds, links)

        val linkCount = json.split("\"from\":").size - 1
        assertEquals(links.size, linkCount,
            "JSON should contain ${links.size} link entries")
    }

    @Test
    fun `json export contains link metadata`() {
        val link = Link("src", "dst",
            config = TransportConfig(
                latencyRangeMs = 10..50,
                packetLossRate = 0.05f,
                type = com.meshlink.simulator.transport.TransportType.BLE
            ))
        val json = exporter.exportJson(listOf("src", "dst"), listOf(link))

        assertTrue(json.contains("\"type\": \"BLE\""), "JSON should include transport type")
        assertTrue(json.contains("\"lossRate\": 0.05"), "JSON should include loss rate")
        assertTrue(json.contains("\"latencyMinMs\": 10"), "JSON should include min latency")
        assertTrue(json.contains("\"latencyMaxMs\": 50"), "JSON should include max latency")
        assertTrue(json.contains("\"enabled\": true"), "JSON should include enabled status")
    }

    @Test
    fun `json export topology matches simulation state`() {
        val sim = MeshSimulator.ring(n = 5, profile = NetworkProfile.PerfectNetwork)
        val json = sim.exportJson()

        // 5 nodes
        val nodeCount = json.split("\"id\":").size - 1
        assertEquals(5, nodeCount, "JSON should reflect 5 simulation nodes")

        // Ring of 5: 10 directed links
        val linkCount = json.split("\"from\":").size - 1
        assertEquals(10, linkCount, "Ring of 5 should have 10 directed links")
    }

    @Test
    fun `exported dot from simulation environment matches nodes`() {
        val sim = MeshSimulator.build {
            nodes(listOf("server", "client1", "client2"))
            topology { ids -> TopologyBuilder.star(ids[0], ids.drop(1)) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val dot = sim.exportDot()

        assertTrue(dot.contains("\"server\""), "DOT should contain server node")
        assertTrue(dot.contains("\"client1\""), "DOT should contain client1 node")
        assertTrue(dot.contains("\"client2\""), "DOT should contain client2 node")
    }

    @Test
    fun `mermaid export from simulation environment is non-empty`() {
        val sim = MeshSimulator.line(n = 3, profile = NetworkProfile.PerfectNetwork)
        val mermaid = sim.exportMermaid()

        assertTrue(mermaid.isNotBlank(), "Mermaid export should not be blank")
        assertTrue(mermaid.contains("graph LR"), "Mermaid should have graph LR header")
    }
}
