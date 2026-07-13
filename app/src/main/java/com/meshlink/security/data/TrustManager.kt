package com.meshlink.security.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.TrustDao
import com.meshlink.database.data.local.TrustEntity
import com.meshlink.di.IoDispatcher
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

@Singleton
class TrustManager @Inject constructor(
    private val trustDao: TrustDao,
    private val securityMonitor: MeshSecurityMonitor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "TrustManager"
        private const val SCORE_MAX = 100
        private const val SCORE_MIN = 0
        private const val SCORE_BLOCKED_THRESHOLD = 20
        private const val SCORE_TRUSTED_THRESHOLD = 80
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    // In-memory cache for fast lookups
    private val trustCache = ConcurrentHashMap<String, TrustEntity>()
    
    // UI state
    private val _trustStates = MutableStateFlow<Map<String, TrustLevel>>(emptyMap())
    val trustStates: StateFlow<Map<String, TrustLevel>> = _trustStates.asStateFlow()

    init {
        scope.launch {
            try {
                val peers = trustDao.getAllPeers()
                peers.forEach { entity ->
                    trustCache[entity.peerId] = entity
                }
                updateTrustStatesFlow()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to load trust database: ${e.message}")
            }
        }
    }

    private fun updateTrustStatesFlow() {
        val currentMap = trustCache.mapValues {
            try {
                TrustLevel.valueOf(it.value.trustLevel)
            } catch (e: Exception) {
                TrustLevel.UNKNOWN
            }
        }
        _trustStates.value = currentMap
    }

    fun getTrustLevel(peerId: String): TrustLevel {
        val entity = trustCache[peerId] ?: return TrustLevel.UNKNOWN
        return try {
            TrustLevel.valueOf(entity.trustLevel)
        } catch (e: Exception) {
            TrustLevel.UNKNOWN
        }
    }

    fun getVerificationStatus(peerId: String): VerificationStatus {
        val entity = trustCache[peerId] ?: return VerificationStatus.NOT_VERIFIED
        return try {
            VerificationStatus.valueOf(entity.verificationStatus)
        } catch (e: Exception) {
            VerificationStatus.NOT_VERIFIED
        }
    }

    /**
     * Called when a peer reconnects or presents a new identity/fingerprint.
     */
    fun updatePeerIdentity(peerId: String, fingerprint: String, deviceUUID: String? = null) {
        scope.launch {
            try {
                val existingEntity = trustCache[peerId]
                
                // Detect Identity Change
                if (existingEntity != null && existingEntity.fingerprint != null && existingEntity.fingerprint != fingerprint) {
                    securityMonitor.reportEvent(peerId, SecurityEvent.IdentityChanged(existingEntity.fingerprint, fingerprint))
                    
                    // Revoke trust automatically
                    val updatedEntity = existingEntity.copy(
                        trustLevel = TrustLevel.REVOKED.name,
                        trustScore = SCORE_MIN,
                        fingerprint = fingerprint,
                        lastSeen = System.currentTimeMillis()
                    )
                    trustDao.updatePeerTrust(updatedEntity)
                    trustCache[peerId] = updatedEntity
                    updateTrustStatesFlow()
                    return@launch
                }

                // Detect Duplicate Fingerprint (another peer using the same fingerprint)
                val duplicate = trustDao.getPeerByFingerprint(fingerprint)
                if (duplicate != null && duplicate.peerId != peerId) {
                    securityMonitor.reportEvent(peerId, SecurityEvent.DuplicateFingerprint(peerId, duplicate.peerId))
                    // Block both peers to be safe
                    blockPeer(peerId)
                    blockPeer(duplicate.peerId)
                    return@launch
                }

                if (existingEntity == null) {
                    val newEntity = TrustEntity(
                        peerId = peerId,
                        deviceUUID = deviceUUID,
                        fingerprint = fingerprint,
                        firstSeen = System.currentTimeMillis(),
                        lastSeen = System.currentTimeMillis(),
                        lastIPAddress = null,
                        lastBLEAddress = null,
                        keyVersion = 1,
                        trustLevel = TrustLevel.DISCOVERED.name,
                        verificationStatus = VerificationStatus.NOT_VERIFIED.name,
                        trustScore = 50, // Start neutral
                        identityHistory = "[]"
                    )
                    trustDao.insertOrUpdatePeerTrust(newEntity)
                    trustCache[peerId] = newEntity
                    updateTrustStatesFlow()
                } else {
                    val updatedEntity = existingEntity.copy(
                        lastSeen = System.currentTimeMillis(),
                        fingerprint = fingerprint,
                        deviceUUID = deviceUUID ?: existingEntity.deviceUUID
                    )
                    trustDao.updatePeerTrust(updatedEntity)
                    trustCache[peerId] = updatedEntity
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Error updating peer identity: ${e.message}")
            }
        }
    }

    /**
     * Called when a peer successfully completes an authenticated action (e.g. handshake, valid packet).
     */
    fun increaseTrustScore(peerId: String, amount: Int = 5) {
        scope.launch {
            try {
                val entity = trustCache[peerId] ?: return@launch
                
                // Don't auto-promote if blocked/revoked
                if (entity.trustLevel == TrustLevel.BLOCKED.name || entity.trustLevel == TrustLevel.REVOKED.name) return@launch

                val newScore = (entity.trustScore + amount).coerceAtMost(SCORE_MAX)
                val newLevel = if (newScore >= SCORE_TRUSTED_THRESHOLD && entity.verificationStatus == VerificationStatus.VERIFIED.name) {
                    TrustLevel.TRUSTED.name
                } else if (entity.trustLevel == TrustLevel.DISCOVERED.name && newScore > 60) {
                    TrustLevel.VERIFIED.name // Soft verification until manual verification
                } else {
                    entity.trustLevel
                }

                trustDao.updateTrustScoreAndLevel(peerId, newScore, newLevel)
                trustCache[peerId] = entity.copy(trustScore = newScore, trustLevel = newLevel)
                updateTrustStatesFlow()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to increase trust score: ${e.message}")
            }
        }
    }

    /**
     * Called when a peer does something bad (e.g. replay attack, invalid signature).
     */
    fun decreaseTrustScore(peerId: String, amount: Int = 10, reason: String = "") {
        scope.launch {
            try {
                val entity = trustCache[peerId] ?: return@launch
                val newScore = (entity.trustScore - amount).coerceAtLeast(SCORE_MIN)
                
                var newLevel = entity.trustLevel
                if (newScore <= SCORE_BLOCKED_THRESHOLD) {
                    newLevel = TrustLevel.BLOCKED.name
                    securityMonitor.reportEvent(peerId, SecurityEvent.BlockedPeer(peerId))
                }

                trustDao.updateTrustScoreAndLevel(peerId, newScore, newLevel)
                trustCache[peerId] = entity.copy(trustScore = newScore, trustLevel = newLevel)
                updateTrustStatesFlow()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to decrease trust score: ${e.message}")
            }
        }
    }

    fun blockPeer(peerId: String) {
        scope.launch {
            try {
                val entity = trustCache[peerId] ?: return@launch
                trustDao.updateTrustScoreAndLevel(peerId, SCORE_MIN, TrustLevel.BLOCKED.name)
                trustCache[peerId] = entity.copy(trustScore = SCORE_MIN, trustLevel = TrustLevel.BLOCKED.name)
                updateTrustStatesFlow()
                securityMonitor.reportEvent(peerId, SecurityEvent.BlockedPeer(peerId))
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to block peer: ${e.message}")
            }
        }
    }
    
    fun verifyDevice(peerId: String) {
        scope.launch {
            try {
                val entity = trustCache[peerId] ?: return@launch
                val updatedEntity = entity.copy(
                    verificationStatus = VerificationStatus.VERIFIED.name,
                    trustLevel = if (entity.trustScore >= 50) TrustLevel.TRUSTED.name else TrustLevel.VERIFIED.name
                )
                trustDao.updatePeerTrust(updatedEntity)
                trustCache[peerId] = updatedEntity
                updateTrustStatesFlow()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to verify device: ${e.message}")
            }
        }
    }
}
