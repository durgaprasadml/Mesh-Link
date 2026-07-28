package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.node.SimulatedNode.NodeConfig
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.security.SimulatedSecurityLayer
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Security validation test suite.
 * Tests encryption round-trips, replay attack rejection, invalid ciphertext handling,
 * expired session rejection, downgrade protection, and encryption latency.
 */
class SecurityValidationTest {

    @Test
    fun `encrypted packet decrypted correctly`() {
        val security = SimulatedSecurityLayer("node-A", seed = 42L)
        security.establishSession("node-B")

        val plaintext = "secure-message-payload"
        val ciphertext = security.encrypt("node-B", plaintext)
        val decrypted = security.decrypt("node-B", ciphertext)

        assertNotNull(decrypted, "Decryption should succeed for correctly encrypted payload")
        assertTrue(decrypted == plaintext, "Decrypted text should match original plaintext")
    }

    @Test
    fun `replay attack rejected`() {
        val security = SimulatedSecurityLayer("node-A")
        security.establishSession("node-B")

        // Valid first packet
        val validSeq = 42L
        val firstValid = security.validateSequence("node-B", validSeq)
        assertTrue(firstValid, "First packet with sequence $validSeq should be accepted")

        // Replay of same sequence number
        val replayAttempt = security.validateSequence("node-B", validSeq)
        assertTrue(!replayAttempt, "Replay of sequence $validSeq should be rejected")
    }

    @Test
    fun `invalid ciphertext rejected by decrypt`() {
        val security = SimulatedSecurityLayer("node-A")
        security.establishSession("node-B")

        val badCiphertext = security.injectInvalidCiphertext()
        val result = security.decrypt("node-B", badCiphertext)
        assertNull(result, "Invalid ciphertext should return null from decrypt")
    }

    @Test
    fun `expired session detected`() {
        val security = SimulatedSecurityLayer("node-A")
        security.establishSession("node-B")
        security.simulateExpiredSession("node-B")

        val isValid = security.isSessionValid("node-B")
        assertTrue(!isValid, "Expired session should be detected as invalid")
    }

    @Test
    fun `downgrade protection enforced in simulation node`() {
        // Node with enforceEncryption=true should drop unencrypted non-SOS packets
        val sim = MeshSimulator.build {
            nodes(listOf("S", "R"))
            topology { ids -> TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
            nodeConfig(NodeConfig(enforceEncryption = true))
        }
        // Send unencrypted packet (encrypted=false, not SOS or KEY_EXCHANGE)
        sim.node("S").sendPacket("R", "unencrypted-data", encrypted = false)
        sim.runUntilQuiet()

        // R enforces encryption — unencrypted packet should be dropped
        val drops = sim.recorder.getDroppedPackets(
            com.meshlink.simulator.metrics.NetworkRecorder.DropReason.ENCRYPTION_FAILED
        )
        assertTrue(drops.isNotEmpty() || sim.node("R").receivedPackets().isEmpty(),
            "Unencrypted packet should be dropped or not delivered under enforceEncryption=true")
    }

    @Test
    fun `encryption latency measured and under 5ms`() {
        val security = SimulatedSecurityLayer("node-A")
        security.establishSession("node-B")

        val latencies = (1..20).map {
            val (_, latencyNanos) = security.encryptWithLatency("node-B", "benchmark-payload-$it")
            latencyNanos
        }
        val avgNs = latencies.average()
        val avgMs = avgNs / 1_000_000.0

        assertTrue(avgMs < 5.0, "Average encryption latency should be < 5ms but was ${avgMs}ms")
    }

    @Test
    fun `different sessions use different keys`() {
        val secA = SimulatedSecurityLayer("nodeA")
        val secB = SimulatedSecurityLayer("nodeB")

        secA.establishSession("nodeB")
        // secB uses different node IDs → different derived key
        secB.establishSession("nodeA")

        val plaintext = "cross-session-test"
        val ctFromA = secA.encrypt("nodeB", plaintext)

        // nodeB cannot decrypt with nodeA's perspective key (different key derivation)
        // This tests that keys are directionally bound to node ID pairs
        val decryptedByB = secB.decrypt("nodeA", ctFromA)
        // Either null (correct - keys differ) or equal (if symmetric key derivation)
        // The simulated key is derived from "$nodeId:$peerId" — directional
        // So secB.decrypt("nodeA", ct) will use "nodeB:nodeA" key, while secA used "nodeA:nodeB"
        // They should differ → decryption fails
        assertNull(decryptedByB,
            "Cross-session decryption should fail (keys are peer-ID-bound)")
    }

    @Test
    fun `encryption round-trip via assertion helper`() {
        val security = SimulatedSecurityLayer("node-test")
        MeshAssertions.assertEncryptionRoundTrip(security, "peer-1", "round-trip-validation")
    }

    @Test
    fun `active peers list tracks established sessions`() {
        val security = SimulatedSecurityLayer("master")
        security.establishSession("peer-1")
        security.establishSession("peer-2")
        security.establishSession("peer-3")

        assertTrue(security.activePeers().size == 3, "Should have 3 active sessions")
        security.removeSession("peer-2")
        assertTrue(security.activePeers().size == 2, "Should have 2 active sessions after removal")
    }
}
