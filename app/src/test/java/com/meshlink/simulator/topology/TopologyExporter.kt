package com.meshlink.simulator.topology

import com.meshlink.simulator.transport.Link
import java.nio.file.Path
import java.nio.file.Files

/**
 * Exports simulation topologies as human-readable graph representations.
 *
 * Supported formats:
 * - **GraphViz DOT** — renderable via `dot -Tpng` or GraphViz online tools
 * - **Mermaid** — embeddable in Markdown/GitHub documentation
 * - **JSON** — machine-readable for tooling and test artifacts
 *
 * Usage:
 * ```kotlin
 * val exporter = TopologyExporter()
 * val dot = exporter.exportDot(nodeIds, links)
 * val mermaid = exporter.exportMermaid(nodeIds, links)
 * val json = exporter.exportJson(nodeIds, links)
 * exporter.writeToFile(dot, Path.of("build/topology.dot"))
 * ```
 */
class TopologyExporter {

    /**
     * Exports the topology as a **GraphViz DOT** directed graph string.
     *
     * Example output:
     * ```dot
     * digraph MeshNetwork {
     *   rankdir=LR;
     *   node [shape=circle, style=filled, fillcolor="#4FC3F7"];
     *   "node0" -> "node1" [label="BLE 5-20ms 0%loss"];
     * }
     * ```
     *
     * @param nodeIds All node IDs in the simulation.
     * @param links   All directed links.
     * @param graphName Name for the digraph (default: "MeshNetwork").
     */
    fun exportDot(
        nodeIds: List<String>,
        links: List<Link>,
        graphName: String = "MeshNetwork"
    ): String = buildString {
        appendLine("digraph $graphName {")
        appendLine("  rankdir=LR;")
        appendLine("  node [shape=circle, style=filled, fillcolor=\"#4FC3F7\", fontname=\"Helvetica\"];")
        appendLine()

        // Nodes
        nodeIds.forEach { id ->
            appendLine("  \"$id\";")
        }
        appendLine()

        // Edges
        links.forEach { link ->
            val label = buildLinkLabel(link)
            val style = if (link.isEnabled) "solid" else "dashed"
            val color = if (link.isEnabled) "black" else "red"
            appendLine(
                "  \"${link.fromNodeId}\" -> \"${link.toNodeId}\" " +
                "[label=\"$label\", style=$style, color=$color];"
            )
        }
        append("}")
    }

    /**
     * Exports the topology as a **Mermaid** graph diagram string.
     *
     * Example output:
     * ```
     * graph LR
     *     node0 -->|BLE 5-20ms| node1
     * ```
     *
     * Suitable for embedding in GitHub README.md or test reports.
     */
    fun exportMermaid(nodeIds: List<String>, links: List<Link>): String = buildString {
        appendLine("graph LR")

        // Declare all nodes with labels
        nodeIds.forEach { id ->
            val safeId = id.replace("-", "_").replace(".", "_")
            appendLine("    $safeId([\"$id\"])")
        }
        appendLine()

        // Edges
        links.forEach { link ->
            val from = link.fromNodeId.replace("-", "_").replace(".", "_")
            val to = link.toNodeId.replace("-", "_").replace(".", "_")
            val label = buildLinkLabel(link)
            val arrow = if (link.isEnabled) "-->" else "-.->"
            appendLine("    $from $arrow|\"$label\"| $to")
        }
    }

    /**
     * Exports the topology as a **JSON** object string.
     *
     * Schema:
     * ```json
     * {
     *   "nodes": [{"id": "node0"}, ...],
     *   "links": [{
     *     "from": "node0", "to": "node1",
     *     "type": "BLE", "latencyMinMs": 5, "latencyMaxMs": 20,
     *     "lossRate": 0.0, "corruptionRate": 0.0, "enabled": true
     *   }, ...]
     * }
     * ```
     */
    fun exportJson(nodeIds: List<String>, links: List<Link>): String = buildString {
        appendLine("{")
        appendLine("  \"nodes\": [")
        nodeIds.forEachIndexed { idx, id ->
            val comma = if (idx < nodeIds.size - 1) "," else ""
            appendLine("    {\"id\": \"$id\"}$comma")
        }
        appendLine("  ],")
        appendLine("  \"links\": [")
        links.forEachIndexed { idx, link ->
            val comma = if (idx < links.size - 1) "," else ""
            appendLine(
                "    {" +
                "\"from\": \"${link.fromNodeId}\", " +
                "\"to\": \"${link.toNodeId}\", " +
                "\"type\": \"${link.config.type}\", " +
                "\"latencyMinMs\": ${link.config.latencyRangeMs.first}, " +
                "\"latencyMaxMs\": ${link.config.latencyRangeMs.last}, " +
                "\"lossRate\": ${link.config.packetLossRate}, " +
                "\"corruptionRate\": ${link.config.corruptionRate}, " +
                "\"enabled\": ${link.isEnabled}" +
                "}$comma"
            )
        }
        appendLine("  ]")
        append("}")
    }

    /**
     * Writes [content] to a file at [path], creating parent directories if needed.
     * Optional convenience — tests can also just assert on the returned string directly.
     */
    fun writeToFile(content: String, path: Path) {
        Files.createDirectories(path.parent)
        Files.write(path, content.toByteArray(Charsets.UTF_8))
    }

    // ── Private ──────────────────────────────────────────────────────────────────

    private fun buildLinkLabel(link: Link): String {
        val type = link.config.type.name
        val latMin = link.config.latencyRangeMs.first
        val latMax = link.config.latencyRangeMs.last
        val loss = (link.config.packetLossRate * 100).toInt()
        return if (!link.isEnabled) "DISABLED" else "$type ${latMin}-${latMax}ms ${loss}%loss"
    }
}
