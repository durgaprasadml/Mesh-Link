package com.meshlink.security.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.DefaultDispatcher
import com.meshlink.domain.model.PeerSecureSession
import com.meshlink.domain.model.SessionState
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class SessionManager @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    private val trustManager: TrustManager,
    private val securityMonitor: MeshSecurityMonitor,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val maintenanceScheduler: com.meshlink.common.maintenance.MaintenanceScheduler,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) {
    private val activeSessions = ConcurrentHashMap<String, PeerSecureSession>()
    private val TAG = "SessionManager"

    init {
        maintenanceScheduler.schedule("SessionCleanup", 60 * 1000L) {
            val now = System.currentTimeMillis()
            activeSessions.values.forEach { session ->
                if (session.state != SessionState.DESTROYED) {
                    if (now > session.expirationTime) {
                        session.state = SessionState.EXPIRED
                        MeshLogger.w(TAG, "Session EXPIRED for peer ${session.peerId}.")
                        removeSession(session.peerId)
                    } else if (now > session.expirationTime - 5 * 60 * 1000L && session.state == SessionState.ACTIVE) {
                        session.state = SessionState.EXPIRING
                    }
                }
            }
        }
    }

    fun getSession(peerId: String): PeerSecureSession? {
        val existing = activeSessions[peerId]
        if (existing != null && existing.state != SessionState.DESTROYED && existing.state != SessionState.EXPIRED) {
            return existing
        }
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
                lastActivity = System.currentTimeMillis(),
                state = SessionState.ACTIVE
            )
            activeSessions[peerId] = restored
            MeshLogger.d(TAG, "Auto-restored session for peer $peerId in ACTIVE state")
            return restored
        }
        return null
    }

    fun createSession(
        peerId: String,
        fingerprint: String,
        sessionVersion: Int,
        cryptoVersion: Int = 1,
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
            cryptoVersion = cryptoVersion,
            verified = verified,
            lastActivity = now,
            state = SessionState.ACTIVE
        )
        activeSessions[peerId] = session
        MeshLogger.d(TAG, "Created new ACTIVE session $derivedSessionId for peer $peerId")
        return session
    }

    fun setSessionState(peerId: String, state: SessionState) {
        val session = activeSessions[peerId]
        if (session != null) {
            session.state = state
            MeshLogger.d(TAG, "Updated session state for $peerId -> $state")
        }
    }

    fun removeSession(peerId: String) {
        val session = activeSessions[peerId]
        if (session != null) {
            session.state = SessionState.DESTROYED
        }
        activeSessions.remove(peerId)
        cryptoManager.removeSharedKey(peerId)
        MeshLogger.d(TAG, "Session DESTROYED & removed for peer $peerId")
    }

    fun getAllSessionPeers(): Set<String> = activeSessions.keys

    fun getActiveSessionsCount(): Int = activeSessions.values.count { it.state == SessionState.ACTIVE || it.state == SessionState.REKEYING }

    fun getAllSessions(): List<PeerSecureSession> = activeSessions.values.toList()

    fun terminateAllSessions() {
        val peers = activeSessions.keys.toList()
        peers.forEach { removeSession(it) }
    }

    /**
     * Generates AAD header containing sid, seq, ts, kv, and pid (packetId).
     */
    fun generateAad(peerId: String, packetId: String = java.util.UUID.randomUUID().toString()): Pair<ByteArray, String>? {
        val trustLevel = trustManager.getTrustLevel(peerId)
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Cannot generate AAD: Peer $peerId is BLOCKED or REVOKED (Level: $trustLevel)")
            securityMonitor.reportEvent(peerId, SecurityEvent.BlockedPeer(peerId))
            return null
        }

        val session = getSession(peerId) ?: return null
        val now = System.currentTimeMillis()
        session.updateActivity(now)

        val seq = session.packetCounter.incrementAndGet()
        session.totalEncryptedPackets.incrementAndGet()

        val aadJson = JSONObject().apply {
            put("sid", session.sessionId)
            put("seq", seq)
            put("ts", now)
            put("kv", session.keyVersion)
            put("pid", packetId)
        }
        val aadString = aadJson.toString()
        val aadBytes = aadString.toByteArray(Charsets.UTF_8)
        val aadBase64 = android.util.Base64.encodeToString(aadBytes, android.util.Base64.NO_WRAP)

        return Pair(aadBytes, "v2|$aadBase64|")
    }

    /**
     * Parses the wrapped payload and validates header metadata.
     * Enforces replay protection, timestamp bounds, and session state.
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
            val pid = json.optString("pid", null)
            val kv = json.optInt("kv", 1)

            val session = activeSessions[peerId]
            if (session == null || session.sessionId != sid || session.state == SessionState.DESTROYED || session.state == SessionState.EXPIRED) {
                MeshLogger.e(TAG, "Rejecting packet: Unknown, mismatched, or expired session for $peerId")
                securityMonitor.reportEvent(peerId, SecurityEvent.SessionHijackAttempt("Session mismatch or expired state"))
                return null
            }

            val now = System.currentTimeMillis()

            if (kv != session.keyVersion) {
                if (kv == session.previousKeyVersion && now <= session.rekeyTimestamp + 60_000) {
                    // Allowed during transition window
                } else if (kv == session.keyVersion + 1 && cryptoManager.hasPeerKey(peerId)) {
                    MeshLogger.d(TAG, "Implicit forward rotation to key version $kv for $peerId")
                    session.previousKeyVersion = session.keyVersion
                    session.keyVersion = kv
                    session.rekeyTimestamp = now
                    session.rotationReason = "implicit_forward_rotation"
                } else {
                    MeshLogger.e(TAG, "Rejecting packet: Invalid keyVersion $kv. Current: ${session.keyVersion}, Prev: ${session.previousKeyVersion}")
                    securityMonitor.reportEvent(peerId, SecurityEvent.DowngradeAttackDetected("Key version mismatch $kv"))
                    return null
                }
            }

            val tsDiff = Math.abs(now - ts)
            if (tsDiff > 300_000) {
                MeshLogger.e(TAG, "Rejecting packet: Stale timestamp (diff = $tsDiff ms)")
                securityMonitor.reportEvent(peerId, SecurityEvent.ReplayAttackDetected("Stale timestamp diff=$tsDiff"))
                return null
            }

            if (session.isReplay(seq, pid)) {
                MeshLogger.e(TAG, "Rejecting packet: Replay detected for sequence $seq (pid=$pid)")
                securityMonitor.reportEvent(peerId, SecurityEvent.ReplayAttackDetected("seq=$seq, pid=$pid"))
                trustManager.decreaseTrustScore(peerId, amount = 10, reason = "Replay Attack Detected")
                return null
            }

            // Valid packet!
            session.markReceived(seq, pid)
            session.updateActivity(now)
            session.totalDecryptedPackets.incrementAndGet()

            return Triple(aadBytes, ciphertext, kv)

        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error parsing AAD JSON: ${e.message}")
            securityMonitor.reportEvent(peerId, SecurityEvent.InvalidSignature("Malformed AAD JSON: ${e.message}"))
            return null
        }
    }
}
