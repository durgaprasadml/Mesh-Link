package com.meshlink.trust

import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.AuditLogDao
import com.meshlink.database.data.local.AuditLogEntity
import com.meshlink.di.ApplicationScope
import com.meshlink.di.IoDispatcher
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class AuditEventType {
    IDENTITY_CREATED,
    IDENTITY_VERIFIED,
    VERIFICATION_REVOKED,
    TRUST_CHANGED,
    COMMUNITY_JOINED,
    COMMUNITY_REMOVED,
    CONFLICT_DETECTED,
    BACKUP_CREATED,
    RESTORE_COMPLETED
}

data class AuditEntry(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: AuditEventType,
    val meshId: String,
    val details: String,
    val previousHash: String,
    val currentHash: String
)

/**
 * Tamper-evident hash-chained identity audit logger.
 * Never stores private keys, message content, or encryption secrets.
 */
@Singleton
class IdentityAuditLog @Inject constructor(
    private val auditLogDao: AuditLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "IdentityAuditLog"
        private const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"
    }

    private val logEntries = CopyOnWriteArrayList<AuditEntry>()
    @Volatile private var lastHash: String = GENESIS_HASH

    init {
        applicationScope.launch(ioDispatcher) {
            try {
                val dbLogs = auditLogDao.getAllAuditLogs()
                dbLogs.reversed().forEach { entity ->
                    val entry = parseEntity(entity)
                    logEntries.add(entry)
                    lastHash = entry.currentHash
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Error initializing AuditLog from database: ${e.message}")
            }
        }
    }

    fun logEvent(eventType: AuditEventType, meshId: String, details: String): AuditEntry {
        val sanitizedDetails = sanitizeDetails(details)
        val timestamp = System.currentTimeMillis()
        val prevHash = lastHash
        val hashInput = "$timestamp:${eventType.name}:$meshId:$sanitizedDetails:$prevHash"
        val currHash = computeSha256(hashInput)

        val entry = AuditEntry(
            timestamp = timestamp,
            eventType = eventType,
            meshId = meshId,
            details = sanitizedDetails,
            previousHash = prevHash,
            currentHash = currHash
        )

        logEntries.add(entry)
        lastHash = currHash

        applicationScope.launch(ioDispatcher) {
            try {
                val entity = AuditLogEntity(
                    timestamp = timestamp,
                    peerId = meshId,
                    eventName = eventType.name,
                    severity = 1,
                    details = "$sanitizedDetails|prev:$prevHash|curr:$currHash",
                    actionTaken = "LOGGED"
                )
                auditLogDao.insertAuditLog(entity)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to persist audit log entry: ${e.message}")
            }
        }
        return entry
    }

    fun verifyChainIntegrity(): Boolean {
        var prev = GENESIS_HASH
        for (entry in logEntries) {
            if (entry.previousHash != prev) {
                MeshLogger.e(TAG, "Audit log chain broken at timestamp ${entry.timestamp}")
                return false
            }
            val expectedHash = computeSha256("${entry.timestamp}:${entry.eventType.name}:${entry.meshId}:${entry.details}:${entry.previousHash}")
            if (entry.currentHash != expectedHash) {
                MeshLogger.e(TAG, "Audit log hash mismatch at timestamp ${entry.timestamp}")
                return false
            }
            prev = entry.currentHash
        }
        return true
    }

    fun getEntries(): List<AuditEntry> = logEntries.toList()

    private fun sanitizeDetails(details: String): String {
        // Redact any potential private key or secret leakage
        return details
            .replace(Regex("(?i)private_?key\\s*[:=]\\s*[^\\s,]+"), "private_key=[REDACTED]")
            .replace(Regex("(?i)secret\\s*[:=]\\s*[^\\s,]+"), "secret=[REDACTED]")
            .replace(Regex("(?i)password\\s*[:=]\\s*[^\\s,]+"), "password=[REDACTED]")
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseEntity(entity: AuditLogEntity): AuditEntry {
        val type = try {
            AuditEventType.valueOf(entity.eventName)
        } catch (e: Exception) {
            AuditEventType.TRUST_CHANGED
        }
        val parts = entity.details.split("|prev:", "|curr:")
        val det = parts.getOrNull(0) ?: entity.details
        val prev = if (parts.size >= 2) parts[1] else GENESIS_HASH
        val curr = if (parts.size >= 3) parts[2] else computeSha256("${entity.timestamp}:${entity.eventName}:${entity.peerId}:$det:$prev")

        return AuditEntry(
            id = entity.id,
            timestamp = entity.timestamp,
            eventType = type,
            meshId = entity.peerId,
            details = det,
            previousHash = prev,
            currentHash = curr
        )
    }
}
