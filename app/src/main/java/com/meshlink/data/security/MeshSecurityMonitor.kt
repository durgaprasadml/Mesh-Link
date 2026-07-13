package com.meshlink.data.security

import android.util.Log
import com.meshlink.data.local.AuditLogDao
import com.meshlink.data.local.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshSecurityMonitor @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    companion object {
        private const val TAG = "MeshSecurityMonitor"
        private const val MAX_AUDIT_LOG_ENTRIES = 1000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // PeerId -> Recent failures
    private val recentSignatureFailures = ConcurrentHashMap<String, Int>()
    private val recentReplayAttempts = ConcurrentHashMap<String, Int>()

    fun reportEvent(peerId: String, event: SecurityEvent) {
        scope.launch {
            try {
                // Log event
                val eventName = event::class.simpleName ?: "Unknown"
                Log.w(TAG, "Security Event [$eventName] for peer $peerId: $event")

                // Keep stats
                when (event) {
                    is SecurityEvent.InvalidSignature -> {
                        val count = recentSignatureFailures.getOrDefault(peerId, 0) + 1
                        recentSignatureFailures[peerId] = count
                    }
                    is SecurityEvent.ReplayAttackDetected -> {
                        val count = recentReplayAttempts.getOrDefault(peerId, 0) + 1
                        recentReplayAttempts[peerId] = count
                    }
                    else -> {}
                }

                // Create audit log
                val detailsJson = JSONObject()
                when (event) {
                    is SecurityEvent.IdentityChanged -> {
                        detailsJson.put("oldFingerprint", event.oldFingerprint)
                        detailsJson.put("newFingerprint", event.newFingerprint)
                    }
                    is SecurityEvent.ReplayAttackDetected -> {
                        detailsJson.put("packetId", event.packetId)
                    }
                    is SecurityEvent.InvalidSignature -> {
                        detailsJson.put("reason", event.reason)
                    }
                    is SecurityEvent.SessionHijackAttempt -> {
                        detailsJson.put("details", event.details)
                    }
                    is SecurityEvent.DuplicateFingerprint -> {
                        detailsJson.put("peerId1", event.peerId1)
                        detailsJson.put("peerId2", event.peerId2)
                    }
                    is SecurityEvent.TrustRevoked -> {
                        detailsJson.put("reason", event.reason)
                    }
                    is SecurityEvent.BlockedPeer -> {
                        detailsJson.put("peerId", event.peerId)
                    }
                    SecurityEvent.UnknownPeer -> {}
                }

                val action = when(event.severity) {
                    5 -> "BLOCKED_AND_DISCONNECTED"
                    4 -> "TRUST_SCORE_DECREASED"
                    3 -> "TRUST_REVOKED"
                    2 -> "WARNING_LOGGED"
                    else -> "IGNORED"
                }

                val auditEntity = AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    peerId = peerId,
                    eventName = eventName,
                    severity = event.severity,
                    details = detailsJson.toString(),
                    actionTaken = action
                )

                auditLogDao.insertAuditLog(auditEntity)
                
                // Enforce log rotation limit
                val count = auditLogDao.getAuditLogCount()
                if (count > MAX_AUDIT_LOG_ENTRIES) {
                    auditLogDao.deleteOldestLogs(count - MAX_AUDIT_LOG_ENTRIES)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report security event: ${e.message}", e)
            }
        }
    }

    fun getSignatureFailureCount(peerId: String): Int {
        return recentSignatureFailures.getOrDefault(peerId, 0)
    }

    fun getReplayAttemptCount(peerId: String): Int {
        return recentReplayAttempts.getOrDefault(peerId, 0)
    }

    fun resetStats(peerId: String) {
        recentSignatureFailures.remove(peerId)
        recentReplayAttempts.remove(peerId)
    }
}
