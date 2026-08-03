package com.meshlink.trust

import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.TrustDao
import com.meshlink.database.data.local.TrustEntity
import com.meshlink.di.ApplicationScope
import com.meshlink.di.IoDispatcher
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data model stored in TrustStore representing peer trust state.
 */
data class TrustedIdentity(
    val meshId: String,
    val publicKey: String,
    val fingerprint: String,
    val trustLevel: TrustLevel,
    val verificationMethod: String,
    val verificationDate: Long,
    val notes: String? = null
)

/**
 * Thread-safe contact trust store maintaining peer verification state.
 * Dual-indexed in-memory cache backed by Room database.
 */
@Singleton
class TrustStore @Inject constructor(
    private val trustDao: TrustDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "TrustStore"
    }

    // Indices
    private val meshIdCache = ConcurrentHashMap<String, TrustedIdentity>()
    private val fingerprintCache = ConcurrentHashMap<String, TrustedIdentity>()

    private val _trustedStateFlow = MutableStateFlow<Map<String, TrustLevel>>(emptyMap())
    val trustedStateFlow: StateFlow<Map<String, TrustLevel>> = _trustedStateFlow.asStateFlow()

    init {
        applicationScope.launch(ioDispatcher) {
            try {
                val entities = trustDao.getAllPeers()
                entities.forEach { entity ->
                    val trusted = entityToModel(entity)
                    meshIdCache[trusted.meshId] = trusted
                    meshIdCache[entity.peerId] = trusted
                    if (trusted.fingerprint.isNotEmpty()) {
                        fingerprintCache[trusted.fingerprint] = trusted
                    }
                }
                updateFlow()
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Error initializing TrustStore from database: ${e.message}")
            }
        }
    }

    private fun updateFlow() {
        _trustedStateFlow.value = meshIdCache.mapValues { it.value.trustLevel }
    }

    fun getTrustLevel(meshId: String): TrustLevel {
        return meshIdCache[meshId]?.trustLevel ?: TrustLevel.UNKNOWN
    }

    fun getIdentity(meshId: String): TrustedIdentity? {
        return meshIdCache[meshId]
    }

    fun getIdentityByFingerprint(fingerprint: String): TrustedIdentity? {
        val formatted = IdentityFingerprint.formatHex(fingerprint.toByteArray())
        return fingerprintCache[fingerprint] ?: fingerprintCache[formatted]
    }

    fun updateTrust(
        meshId: String,
        publicKey: String,
        trustLevel: TrustLevel,
        verificationMethod: String,
        notes: String? = null
    ) {
        val fingerprint = IdentityFingerprint.compute(publicKey)
        val now = System.currentTimeMillis()
        val identity = TrustedIdentity(
            meshId = meshId,
            publicKey = publicKey,
            fingerprint = fingerprint,
            trustLevel = trustLevel,
            verificationMethod = verificationMethod,
            verificationDate = now,
            notes = notes
        )

        meshIdCache[meshId] = identity
        fingerprintCache[fingerprint] = identity
        updateFlow()

        applicationScope.launch(ioDispatcher) {
            try {
                val entity = TrustEntity(
                    peerId = meshId,
                    deviceUUID = null,
                    fingerprint = fingerprint,
                    firstSeen = now,
                    lastSeen = now,
                    lastIPAddress = null,
                    lastBLEAddress = null,
                    keyVersion = 1,
                    trustLevel = trustLevel.name,
                    verificationStatus = if (trustLevel.isVerified()) "VERIFIED" else "NOT_VERIFIED",
                    trustScore = if (trustLevel.isVerified()) 100 else 50,
                    identityHistory = "[]"
                )
                trustDao.insertOrUpdatePeerTrust(entity)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to persist trust update for $meshId: ${e.message}")
            }
        }
    }

    fun revokeTrust(meshId: String) {
        val existing = meshIdCache[meshId] ?: return
        val revoked = existing.copy(trustLevel = TrustLevel.REVOKED)
        meshIdCache[meshId] = revoked
        fingerprintCache[existing.fingerprint] = revoked
        updateFlow()

        applicationScope.launch(ioDispatcher) {
            try {
                trustDao.updateTrustScoreAndLevel(meshId, 0, TrustLevel.REVOKED.name)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to persist trust revocation for $meshId: ${e.message}")
            }
        }
    }

    fun getVerifiedUsers(): List<TrustedIdentity> {
        return meshIdCache.values.filter { it.trustLevel.isVerified() }.distinctBy { it.meshId }
    }

    fun getAllIdentities(): List<TrustedIdentity> {
        return meshIdCache.values.distinctBy { it.meshId }
    }

    private fun entityToModel(entity: TrustEntity): TrustedIdentity {
        val level = try {
            TrustLevel.valueOf(entity.trustLevel)
        } catch (e: Exception) {
            TrustLevel.UNKNOWN
        }
        val fp = entity.fingerprint ?: ""
        return TrustedIdentity(
            meshId = entity.peerId,
            publicKey = "",
            fingerprint = fp,
            trustLevel = level,
            verificationMethod = entity.verificationStatus,
            verificationDate = entity.lastSeen,
            notes = null
        )
    }
}
