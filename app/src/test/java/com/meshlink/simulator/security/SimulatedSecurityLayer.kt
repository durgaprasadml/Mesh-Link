package com.meshlink.simulator.security

import com.meshlink.domain.model.PeerSecureSession
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM-safe simulated security layer for mesh simulation tests.
 *
 * Uses standard JCE (javax.crypto) with AES-GCM instead of Android Keystore
 * or `android.util.Base64`, allowing all security tests to run on the JVM
 * without Robolectric.
 *
 * Implements:
 * - AES-256-GCM symmetric encryption per peer session
 * - Replay protection via the real [PeerSecureSession] sliding window
 * - Session establishment and expiry
 * - Encryption latency measurement for [NodeMetrics]
 *
 * Failure injection methods for security test scenarios:
 * - [injectReplayPacket] — replays a captured sequence number
 * - [injectInvalidCiphertext] — returns garbage bytes as ciphertext
 * - [simulateExpiredSession] — sets session expiration to epoch 0
 * - [simulateDowngradeAttempt] — marks a packet as unencrypted
 *
 * @param nodeId The mesh ID of the node that owns this security layer.
 * @param seed   Key generation seed for reproducible test keys.
 */
class SimulatedSecurityLayer(
    val nodeId: String,
    private val seed: Long = 42L
) {
    private val random = SecureRandom()
    private val sessions = ConcurrentHashMap<String, PeerSecureSession>()
    private val peerKeys = ConcurrentHashMap<String, SecretKey>()

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    // ── Session Management ────────────────────────────────────────────────────────

    /**
     * Establishes a symmetric AES-256 session key with [peerId].
     * In a real node this would be done via ECDH key exchange.
     * The simulator uses a pre-shared key derived from the peer IDs for reproducibility.
     */
    fun establishSession(peerId: String): PeerSecureSession {
        // Deterministic key from both peer IDs (pre-shared for test convenience)
        val keyMaterial = "$nodeId:$peerId".toByteArray(Charsets.UTF_8).take(32).toByteArray()
            .let { it + ByteArray(32 - it.size) } // pad to 32 bytes
        val secretKey = SecretKeySpec(keyMaterial, "AES")
        peerKeys[peerId] = secretKey

        val session = PeerSecureSession(
            peerId = peerId,
            sessionId = UUID.randomUUID().toString().take(16),
            fingerprint = "sim:$nodeId-$peerId",
            sessionStart = System.currentTimeMillis(),
            sessionVersion = 2,
            cryptoVersion = 1,
            verified = true,
            lastActivity = System.currentTimeMillis()
        )
        sessions[peerId] = session
        return session
    }

    /** Returns the active session for [peerId], or null if not established. */
    fun getSession(peerId: String): PeerSecureSession? = sessions[peerId]

    /** Returns all active session peer IDs. */
    fun activePeers(): Set<String> = sessions.keys.toSet()

    /** Removes a session. */
    fun removeSession(peerId: String) {
        sessions.remove(peerId)
        peerKeys.remove(peerId)
    }

    // ── Encryption / Decryption ───────────────────────────────────────────────────

    /**
     * Encrypts [plaintext] for [peerId] using AES-256-GCM.
     * Auto-establishes a session if one does not exist.
     *
     * @return Base64-encoded ciphertext with prepended IV: `"<base64-iv>.<base64-ciphertext>"`
     * @throws IllegalStateException if encryption fails.
     */
    fun encrypt(peerId: String, plaintext: String): String {
        if (!peerKeys.containsKey(peerId)) establishSession(peerId)
        val key = peerKeys[peerId]!!

        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val ivB64 = Base64.getEncoder().encodeToString(iv)
        val ctB64 = Base64.getEncoder().encodeToString(cipherBytes)
        return "$ivB64.$ctB64"
    }

    /**
     * Decrypts [ciphertext] received from [peerId].
     * @return The plaintext, or null if decryption fails (bad key, corrupted, replay).
     */
    fun decrypt(peerId: String, ciphertext: String): String? {
        val key = peerKeys[peerId] ?: return null
        return try {
            val parts = ciphertext.split(".")
            if (parts.size != 2) return null
            val iv = Base64.getDecoder().decode(parts[0])
            val ctBytes = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ctBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypts [plaintext] and measures how long encryption took in nanoseconds.
     * @return Pair of (ciphertext, latencyNanos).
     */
    fun encryptWithLatency(peerId: String, plaintext: String): Pair<String, Long> {
        val start = System.nanoTime()
        val ct = encrypt(peerId, plaintext)
        return ct to (System.nanoTime() - start)
    }

    // ── Replay Protection ─────────────────────────────────────────────────────────

    /**
     * Validates a sequence number for replay protection using the real
     * [PeerSecureSession] sliding window (64-packet window).
     *
     * @return true if the sequence is valid (NOT a replay).
     */
    fun validateSequence(peerId: String, sequence: Long): Boolean {
        val session = sessions[peerId] ?: return false
        return if (session.isReplay(sequence)) {
            false
        } else {
            session.markReceived(sequence)
            true
        }
    }

    // ── Failure Injection API ─────────────────────────────────────────────────────

    /**
     * Returns a ciphertext that reuses a previously used sequence number.
     * Used in [SecurityValidationTest] to verify replay rejection.
     */
    fun injectReplayPacket(peerId: String, originalSequence: Long): String {
        // Return a specially-tagged payload that looks like a valid encrypted packet
        // but carries an old sequence. The node's replay window will reject it.
        return "REPLAY:$peerId:$originalSequence"
    }

    /**
     * Returns garbage bytes as a Base64 ciphertext string.
     * Decryption will fail with a bad-MAC / tag mismatch.
     */
    fun injectInvalidCiphertext(): String {
        val garbage = ByteArray(32).also { random.nextBytes(it) }
        return Base64.getEncoder().encodeToString(garbage) + ".INVALID"
    }

    /**
     * Forces the session with [peerId] to appear expired (expirationTime set to epoch 0).
     * Mutates lastActivity directly since PeerSecureSession contains AtomicLong fields
     * that are not safe to copy via the generated data class copy().
     */
    fun simulateExpiredSession(peerId: String) {
        val session = sessions[peerId] ?: establishSession(peerId)
        // Mutate mutable fields directly (safe — both are var)
        session.lastActivity = 0L
        session.expirationTime = 0L
    }

    /**
     * Checks whether the session with [peerId] is still valid (not expired).
     */
    fun isSessionValid(peerId: String): Boolean {
        val session = sessions[peerId] ?: return false
        return System.currentTimeMillis() < session.expirationTime
    }

    override fun toString(): String = "SimulatedSecurityLayer(node=$nodeId, sessions=${sessions.size})"
}
