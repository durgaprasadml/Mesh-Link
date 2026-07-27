package com.meshlink.securitytest

import com.meshlink.simulator.node.SimulatedNode
import com.meshlink.simulator.security.SimulatedSecurityLayer
import java.util.Random

/**
 * Encapsulates deterministic failure injection and MITM packet manipulation.
 * Exposes methods to simulate various attacks against the Mesh Link protocol.
 */
class AttackSimulator(private val seed: Long = SecurityTestFixtures.DEFAULT_SEED) {

    private val random = Random(seed)
    
    // In-memory capture of packets for replay/MITM
    private val capturedPackets = mutableListOf<String>()

    fun capturePacket(ciphertext: String) {
        capturedPackets.add(ciphertext)
    }

    /**
     * Captures and replays an old sequence number payload format.
     */
    fun injectReplay(security: SimulatedSecurityLayer, targetPeerId: String, oldSequence: Long): String {
        return security.injectReplayPacket(targetPeerId, oldSequence)
    }

    /**
     * Simulates a replay attack across different sessions.
     */
    fun injectCrossSessionReplay(securityA: SimulatedSecurityLayer, targetB: String, capturedCiphertext: String): String {
        // Just replay the raw ciphertext to the target (which may have rotated keys)
        return capturedCiphertext
    }

    /**
     * Injects an out of order packet deterministically.
     */
    fun injectOutOfOrder(sequence1: Long, sequence2: Long): Pair<Long, Long> {
        return Pair(sequence2, sequence1)
    }

    /**
     * Modifies the sender ID to simulate identity spoofing.
     */
    fun injectIdentitySpoofing(payload: String): String {
        return "SPOOFED_ID:$payload"
    }

    /**
     * Strips encryption headers from a packet.
     */
    fun injectDowngrade(plaintext: String): String {
        // Returning plaintext directly bypasses encryption layers in simulation if passed raw
        return plaintext
    }

    /**
     * Mutates the payload or AES-GCM tag.
     */
    fun corruptCiphertext(security: SimulatedSecurityLayer, partial: Boolean = false): String {
        if (partial) {
            val validCipher = security.injectInvalidCiphertext()
            // We just flip some bits to simulate partial corruption
            val chars = validCipher.toCharArray()
            if (chars.isNotEmpty()) {
                val idx = random.nextInt(chars.size)
                chars[idx] = (chars[idx] + 1).toChar()
            }
            return String(chars)
        }
        return security.injectInvalidCiphertext()
    }

    /**
     * Mutates expirationTime on a PeerSecureSession.
     */
    fun forceSessionExpiry(security: SimulatedSecurityLayer, peerId: String) {
        security.simulateExpiredSession(peerId)
    }

    /**
     * Simulates a session exhaustion attack by opening thousands of sessions.
     */
    fun exhaustSessions(security: SimulatedSecurityLayer) {
        for (i in 1..1000) {
            security.establishSession("exhaustion-peer-$i")
        }
    }

    /**
     * Generates malformed packets deterministically based on seed.
     */
    fun fuzzPacketHeaders(): String {
        val formats = listOf(
            "",
            "GARBAGE_NO_DOTS",
            "TOO.MANY.DOTS.HERE",
            "A".repeat(10000), // Oversized
            "INVALID_BASE64_#*(@&"
        )
        return formats[random.nextInt(formats.size)]
    }
}
