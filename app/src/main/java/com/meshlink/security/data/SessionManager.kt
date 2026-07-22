package com.meshlink.security.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.ble.data.PeerSecureSession
import com.meshlink.di.DefaultDispatcher
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest
@Singleton
class SessionManager @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    private val trustManager: com.meshlink.security.data.TrustManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val activeSessions = ConcurrentHashMap<String, PeerSecureSession>()
    private val scope = CoroutineScope(defaultDispatcher + SupervisorJob())
    private val TAG = "SessionManager"

    init {
        startCleanupRoutine()
    }

    fun getSession(peerId: String): PeerSecureSession? {
        val existing = activeSessions[peerId]
        if (existing != null) return existing
        if (cryptoManager.hasPeerKey(peerId)) {
            val fingerprint = cryptoManager.getPeerSigningKey(peerId)?.let { cryptoManager.getDeviceFingerprint(it) } ?: "ESTABLISHED"
            val localFingerprint = cryptoManager.getLocalFingerprint()
            val sorted = listOf(localFingerprint, fingerprint).sorted()
            val combined = sorted[0] + ":" + sorted[1]
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
            val derivedSessionId = hash.joinToString("") { "%02x".format(it) }.take(16)

            val restored = PeerSecureSession(
                peerId = peerId,
                sessionId = derivedSessionId,
                fingerprint = fingerprint,
                sessionStart = System.currentTimeMillis(),
                sessionVersion = 2,
                verified = true,
                lastActivity = System.currentTimeMillis()
            )
            activeSessions[peerId] = restored
            MeshLogger.d(TAG, "Auto-restored session for peer $peerId from persisted keys")
            return restored
        }
        return null
    }

    fun createSession(
        peerId: String,
        fingerprint: String,
        sessionVersion: Int,
        verified: Boolean
    ): PeerSecureSession {
        val localFingerprint = cryptoManager.getLocalFingerprint()
        val sorted = listOf(localFingerprint, fingerprint).sorted()
        val combined = sorted[0] + ":" + sorted[1]
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
        val derivedSessionId = hash.joinToString("") { "%02x".format(it) }.take(16)

        val now = System.currentTimeMillis()
        val session = PeerSecureSession(
            peerId = peerId,
            sessionId = derivedSessionId,
            fingerprint = fingerprint,
            sessionStart = now,
            sessionVersion = sessionVersion,
            verified = verified,
            lastActivity = now
        )
        activeSessions[peerId] = session
        return session
    }

    fun removeSession(peerId: String) {
        activeSessions.remove(peerId)
        cryptoManager.removeSharedKey(peerId)
    }

    fun getAllSessionPeers(): Set<String> = activeSessions.keys

    fun terminateAllSessions() {
        val peers = activeSessions.keys.toList()
        peers.forEach { removeSession(it) }
    }

    private fun startCleanupRoutine() {
        scope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes
                val now = System.currentTimeMillis()
                val expiredPeers = activeSessions.entries.filter { it.value.expirationTime < now }.map { it.key }
                
                expiredPeers.forEach { peerId ->
                    MeshLogger.w(TAG, "Session expired for peer $peerId. Removing.")
                    removeSession(peerId)
                }
            }
        }
    }

    /**
     * Generates AAD containing sessionId, sequenceNumber, timestamp.
     * Returns Pair(AAD_Bytes, "v2|Base64(AAD)|")
     */
    fun generateAad(peerId: String): Pair<ByteArray, String>? {
        val trustLevel = trustManager.getTrustLevel(peerId)
        if (trustLevel == com.meshlink.security.data.TrustLevel.BLOCKED || trustLevel == com.meshlink.security.data.TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Cannot generate AAD: Peer $peerId is BLOCKED or REVOKED (Level: $trustLevel)")
            return null
        }

        val session = activeSessions[peerId] ?: return null
        val now = System.currentTimeMillis()
        session.updateActivity(now)

        val seq = session.packetCounter.incrementAndGet()
        session.totalEncryptedPackets.incrementAndGet()
        
        val aadJson = JSONObject().apply {
            put("sid", session.sessionId)
            put("seq", seq)
            put("ts", now)
            put("kv", session.keyVersion)
        }
        val aadString = aadJson.toString()
        val aadBytes = aadString.toByteArray(Charsets.UTF_8)
        val aadBase64 = android.util.Base64.encodeToString(aadBytes, android.util.Base64.NO_WRAP)
        
        return Pair(aadBytes, "v2|$aadBase64|")
    }

    /**
     * Parses the wrapped payload, validates AAD (timestamp, seq, sessionId, keyVersion).
     * Returns a Triple of (AAD bytes, Ciphertext string, keyVersion) if valid, or null if invalid.
     */
    fun validateAndUnwrap(peerId: String, payload: String): Triple<ByteArray, String, Int>? {
        if (!payload.startsWith("v2|")) return null

        val parts = payload.split("|")
        if (parts.size != 3) return null

        val aadBase64 = parts[1]
        val ciphertext = parts[2]

        val aadBytes = android.util.Base64.decode(aadBase64, android.util.Base64.NO_WRAP)
        val aadString = String(aadBytes, Charsets.UTF_8)
        
        try {
            val json = JSONObject(aadString)
            val sid = json.getString("sid")
            val seq = json.getLong("seq")
            val ts = json.getLong("ts")

            val kv = json.optInt("kv", 1) // default to 1 for backward compatibility

            val session = activeSessions[peerId]
            if (session == null || session.sessionId != sid) {
                MeshLogger.e(TAG, "Rejecting packet: Unknown or mismatched session")
                return null
            }

            val now = System.currentTimeMillis()

            if (kv != session.keyVersion) {
                if (kv == session.previousKeyVersion && now <= session.rekeyTimestamp + 60_000) {
                    // Allowed during transition window
                } else {
                    MeshLogger.e(TAG, "Rejecting packet: Invalid keyVersion $kv. Current: ${session.keyVersion}, Prev: ${session.previousKeyVersion}")
                    return null
                }
            }

            val tsDiff = Math.abs(now - ts)
            if (tsDiff > 300_000) {
                MeshLogger.e(TAG, "Rejecting packet: Stale timestamp (diff = $tsDiff ms)")
                return null
            }

            if (session.isReplay(seq)) {
                MeshLogger.e(TAG, "Rejecting packet: Replay detected for sequence $seq")
                securityMonitor.reportEvent(peerId, com.meshlink.security.data.SecurityEvent.ReplayAttackDetected("$seq"))
                trustManager.decreaseTrustScore(peerId, amount = 10, reason = "Replay Attack Detected")
                return null
            }

            // Valid! Mark received and update activity.
            session.markReceived(seq)
            session.updateActivity(now)
            session.totalDecryptedPackets.incrementAndGet()

            return Triple(aadBytes, ciphertext, kv)

        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error parsing AAD JSON: ${e.message}")
            return null
        }
    }
}
