package com.meshlink.security.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.AuditLogDao
import com.meshlink.database.data.local.AuditLogEntity
import com.meshlink.di.IoDispatcher
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class SecurityMonitorState(
    val totalReplayAttempts: Int = 0,
    val totalAuthFailures: Int = 0,
    val totalFailedRekeys: Int = 0,
    val totalKeyRotations: Int = 0,
    val totalTamperedPackets: Int = 0,
    val totalExpiredSessions: Int = 0,
    val totalUnknownDevices: Int = 0,
    val totalSignatureFailures: Int = 0
)

@Singleton
class MeshSecurityMonitor @Inject constructor(
    private val auditLogDao: AuditLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MeshSecurityMonitor"
        private const val MAX_AUDIT_LOG_ENTRIES = 1000
    }

    private val recentSignatureFailures = ConcurrentHashMap<String, Int>()
    private val recentReplayAttempts = ConcurrentHashMap<String, Int>()

    private val replayCount = AtomicInteger(0)
    private val authFailureCount = AtomicInteger(0)
    private val failedRekeyCount = AtomicInteger(0)
    private val keyRotationCount = AtomicInteger(0)
    private val tamperedPacketCount = AtomicInteger(0)
    private val expiredSessionCount = AtomicInteger(0)
    private val unknownDeviceCount = AtomicInteger(0)
    private val signatureFailureCount = AtomicInteger(0)

    private val _metricsState = MutableStateFlow(SecurityMonitorState())
    val metricsState: StateFlow<SecurityMonitorState> = _metricsState.asStateFlow()

    fun reportEvent(peerId: String, event: SecurityEvent) {
        applicationScope.launch {
            try {
                val eventName = event::class.simpleName ?: "Unknown"
                MeshLogger.w(TAG, "Security Event [$eventName] for peer $peerId: $event")

                when (event) {
                    is SecurityEvent.InvalidSignature -> {
                        val count = recentSignatureFailures.getOrDefault(peerId, 0) + 1
                        recentSignatureFailures[peerId] = count
                        signatureFailureCount.incrementAndGet()
                        authFailureCount.incrementAndGet()
                    }
                    is SecurityEvent.ReplayAttackDetected -> {
                        val count = recentReplayAttempts.getOrDefault(peerId, 0) + 1
                        recentReplayAttempts[peerId] = count
                        replayCount.incrementAndGet()
                    }
                    is SecurityEvent.SessionHijackAttempt -> {
                        authFailureCount.incrementAndGet()
                        tamperedPacketCount.incrementAndGet()
                    }
                    is SecurityEvent.DowngradeAttackDetected -> {
                        tamperedPacketCount.incrementAndGet()
                    }
                    is SecurityEvent.UnknownPeer -> {
                        unknownDeviceCount.incrementAndGet()
                    }
                    else -> {}
                }

                updateMetricsState()

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
                    is SecurityEvent.DowngradeAttackDetected -> {
                        detailsJson.put("details", event.details)
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

                val count = auditLogDao.getAuditLogCount()
                if (count > MAX_AUDIT_LOG_ENTRIES) {
                    auditLogDao.deleteOldestLogs(count - MAX_AUDIT_LOG_ENTRIES)
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to report security event: ${e.message}", e)
            }
        }
    }

    fun recordKeyRotation() {
        keyRotationCount.incrementAndGet()
        updateMetricsState()
    }

    fun recordFailedRekey() {
        failedRekeyCount.incrementAndGet()
        updateMetricsState()
    }

    fun recordExpiredSession() {
        expiredSessionCount.incrementAndGet()
        updateMetricsState()
    }

    private fun updateMetricsState() {
        _metricsState.value = SecurityMonitorState(
            totalReplayAttempts = replayCount.get(),
            totalAuthFailures = authFailureCount.get(),
            totalFailedRekeys = failedRekeyCount.get(),
            totalKeyRotations = keyRotationCount.get(),
            totalTamperedPackets = tamperedPacketCount.get(),
            totalExpiredSessions = expiredSessionCount.get(),
            totalUnknownDevices = unknownDeviceCount.get(),
            totalSignatureFailures = signatureFailureCount.get()
        )
    }

    fun getSignatureFailureCount(peerId: String): Int = recentSignatureFailures.getOrDefault(peerId, 0)
    fun getReplayAttemptCount(peerId: String): Int = recentReplayAttempts.getOrDefault(peerId, 0)

    fun resetStats(peerId: String) {
        recentSignatureFailures.remove(peerId)
        recentReplayAttempts.remove(peerId)
    }
}
