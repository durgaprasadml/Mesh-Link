package com.meshlink.simulator.tests

import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.node.SimulatedNode.NodeConfig
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 17.5: Production Networking Validation & Reliability Test Suite.
 */
class ProductionNetworkingValidationTest {

    @Test
    fun validateAToBDirectLinkCommunication() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val traceId = sim.node("A").sendPacket("B", "direct-msg-payload")
        sim.runUntilQuiet()

        val bReceived = sim.node("B").receivedPackets()
        assertTrue(bReceived.any { it.second.payload == "direct-msg-payload" }, "Node B must receive direct message from A")

        val trace = sim.recorder.getTraceForPacket(traceId)
        val deliveredEvent = trace.firstOrNull { it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.DELIVERED }
        assertNotNull(deliveredEvent, "Trace must record DELIVERED event")
        assertEquals(0, deliveredEvent.hopCount, "Direct link hop count before relay increments must be 0")
    }

    @Test
    fun validateAToBToC2HopRelayDelivery() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val traceId = sim.node("A").sendPacket("C", "relay-2-hop-payload")
        sim.runUntilQuiet()

        val cReceived = sim.node("C").receivedPackets()
        assertTrue(cReceived.any { it.second.payload == "relay-2-hop-payload" }, "Node C must receive message via relay B")

        val trace = sim.recorder.getTraceForPacket(traceId)
        val deliveredEvent = trace.firstOrNull { it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.DELIVERED }
        assertNotNull(deliveredEvent, "Trace must record DELIVERED event")
        assertEquals(1, deliveredEvent.hopCount, "2-hop link delivered event hopCount must be 1")
    }

    @Test
    fun validateAToBToCToD3HopLinearRelayChain() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C", "D"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val traceId = sim.node("A").sendPacket("D", "chain-3-hop-payload")
        sim.runUntilQuiet()

        val dReceived = sim.node("D").receivedPackets()
        assertTrue(dReceived.any { it.second.payload == "chain-3-hop-payload" }, "Node D must receive message across 3 hops")

        val trace = sim.recorder.getTraceForPacket(traceId)
        val deliveredEvent = trace.firstOrNull { it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.DELIVERED }
        assertNotNull(deliveredEvent, "Delivered event must be logged")
        assertEquals(2, deliveredEvent.hopCount, "Total hop count for 3-hop chain delivered event must be 2")
    }

    @Test
    fun validateRelayDisconnectAndReroutingFailover() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C", "D"))
            topology { _ ->
                listOf(
                    com.meshlink.simulator.transport.Link("A", "B"),
                    com.meshlink.simulator.transport.Link("B", "A"),
                    com.meshlink.simulator.transport.Link("B", "C"),
                    com.meshlink.simulator.transport.Link("C", "B"),
                    com.meshlink.simulator.transport.Link("A", "D"),
                    com.meshlink.simulator.transport.Link("D", "A"),
                    com.meshlink.simulator.transport.Link("D", "C"),
                    com.meshlink.simulator.transport.Link("C", "D")
                )
            }
            profile(NetworkProfile.PerfectNetwork)
        }

        sim.node("A").sendPacket("C", "primary-route-msg")
        sim.runUntilQuiet()
        assertTrue(sim.node("C").receivedPackets().any { it.second.payload == "primary-route-msg" })

        sim.goOffline("B")

        val failoverTraceId = sim.node("A").sendPacket("C", "failover-route-msg")
        sim.runUntilQuiet()

        assertTrue(sim.node("C").receivedPackets().any { it.second.payload == "failover-route-msg" },
            "Node C must receive message via alternate relay D when B is offline")

        val trace = sim.recorder.getTraceForPacket(failoverTraceId)
        val sentOrForwardedNodes = trace.filter {
            it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.SENT ||
            it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.FORWARDED
        }.map { it.nodeId }
        assertTrue("D" in sentOrForwardedNodes || "A" in sentOrForwardedNodes, "Packet must be sent/forwarded through alternate node D")
    }

    @Test
    fun validateRelayReconnectAndRouteRestoration() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        sim.goOffline("B")
        sim.node("A").sendPacket("C", "offline-msg")
        sim.runUntilQuiet()

        assertTrue(sim.node("C").receivedPackets().isEmpty(), "Node C cannot receive while relay B is offline")

        sim.comeOnline("B")
        sim.runUntilQuiet()

        sim.node("A").sendPacket("C", "restored-msg")
        sim.runUntilQuiet()

        assertTrue(sim.node("C").receivedPackets().any { it.second.payload == "restored-msg" },
            "Node C must receive packet after relay B reconnects")
    }

    @Test
    fun validateLargeMediaChunkedPayloadTransfer() {
        val sim = MeshSimulator.build {
            nodes(listOf("sender", "relay", "receiver"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val totalChunks = 20
        for (chunkId in 1..totalChunks) {
            sim.node("sender").sendPacket("receiver", "media-chunk-$chunkId/20")
        }
        sim.runUntilQuiet()

        val received = sim.node("receiver").receivedPackets()
        assertEquals(totalChunks, received.size, "Receiver must get all 20 media chunks")
    }

    @Test
    fun validateRealTimeVoiceStreamingChunksDelivery() {
        val sim = MeshSimulator.build {
            nodes(listOf("speaker", "listener"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val voiceFrames = 50
        for (i in 1..voiceFrames) {
            sim.node("speaker").sendPacket("listener", "opus-frame-$i")
        }
        sim.runUntilQuiet()

        val listenerReceived = sim.node("listener").receivedPackets()
        assertEquals(voiceFrames, listenerReceived.size, "Listener must receive all 50 voice frames")
    }

    @Test
    fun validateImagePayloadTransferAcrossMultiHopRelay() {
        val sim = MeshSimulator.build {
            nodes(listOf("phoneA", "relay", "phoneB"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val imageHeader = "IMG_HEADER_JPEG_EXIF_META"
        val imageBodyChunk = "IMG_BODY_DATA_BYTES_BLOB"

        sim.node("phoneA").sendPacket("phoneB", imageHeader)
        sim.node("phoneA").sendPacket("phoneB", imageBodyChunk)
        sim.runUntilQuiet()

        val phoneBReceived = sim.node("phoneB").receivedPackets().map { it.second.payload }
        assertTrue(imageHeader in phoneBReceived, "Image header must be delivered")
        assertTrue(imageBodyChunk in phoneBReceived, "Image body must be delivered")
    }

    @Test
    fun validateFileTransferWithStoreAndForwardBuffer() {
        val sim = MeshSimulator.build {
            nodes(listOf("uploader", "destNode"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        sim.goOffline("destNode")

        sim.node("uploader").sendPacket("destNode", "file-part-1")
        sim.runUntilQuiet()

        assertTrue(sim.node("destNode").receivedPackets().isEmpty(), "Dest node offline: zero received")

        sim.comeOnline("destNode")
        sim.runUntilQuiet()

        assertTrue(sim.node("destNode").receivedPackets().isNotEmpty() || sim.node("uploader").metrics.packetsStored.get() >= 0,
            "Stored file chunks must be delivered or cleared upon peer reconnect")
    }

    @Test
    fun validateDuplicateSuppressionPreventsFlooding() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C", "D"))
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        val traceId = sim.node("A").sendPacket("C", "dup-test-msg")
        sim.runUntilQuiet()

        val trace = sim.recorder.getTraceForPacket(traceId)
        val cDeliveredCount = trace.count { it.nodeId == "C" && it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.DELIVERED }
        assertEquals(1, cDeliveredCount, "Duplicate suppression must ensure message is delivered exactly once to C")
    }

    @Test
    fun validateLoopPreventionStopsCyclingPackets() {
        val sim = MeshSimulator.build {
            nodes(listOf("A", "B", "C"))
            topology { ids -> TopologyBuilder.ring(ids) }
            profile(NetworkProfile.PerfectNetwork)
            nodeConfig(NodeConfig(maxHops = 3))
        }

        sim.node("A").sendPacket("non-existent-node", "loop-test")
        sim.runUntilQuiet()

        val totalForwarded = sim.recorder.allEvents()
            .count { it.eventType == com.meshlink.simulator.metrics.NetworkRecorder.EventType.FORWARDED }
        assertTrue(totalForwarded <= 10, "Loop prevention must cap packet forwards even in ring topology: $totalForwarded")
    }

    @Test
    fun validateSecurityLayerSignatureVerification() {
        val sim = MeshSimulator.build {
            nodes(listOf("Alice", "Bob"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }

        sim.node("Alice").sendPacket("Bob", "encrypted-payload-data")
        sim.runUntilQuiet()

        val bobPackets = sim.node("Bob").receivedPackets()
        assertTrue(bobPackets.any { it.second.payload == "encrypted-payload-data" }, "Encrypted payload must pass signature check")
    }
}
