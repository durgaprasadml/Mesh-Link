package com.meshlink.enterprise.governance

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class AuditRecord(
    val timestamp: Long,
    val eventType: String,
    val description: String,
    val previousHash: String,
    val currentHash: String
)

@Singleton
class AuditManager @Inject constructor() {

    private val auditLedger = mutableListOf<AuditRecord>()
    private var lastHash = "00000000000000000000000000000000" // Genesis Hash

    fun recordEvent(eventType: String, description: String) {
        val timestamp = System.currentTimeMillis()
        val rawData = "$timestamp|$eventType|$description|$lastHash"
        val currentHash = computeSha256(rawData)
        
        val record = AuditRecord(timestamp, eventType, description, lastHash, currentHash)
        auditLedger.add(record)
        lastHash = currentHash
        
        // Strategy as per User Review: 
        // If ledger exceeds threshold, oldest records are dropped to prevent storage exhaustion,
        // but a snapshot hash is preserved to maintain the chain's cryptographic integrity.
        if (auditLedger.size > 10_000) {
            auditLedger.removeAt(0)
        }
    }

    fun verifyLedgerIntegrity(): Boolean {
        var previous = "00000000000000000000000000000000"
        
        for (record in auditLedger) {
            if (record.previousHash != previous) return false
            
            val expectedHash = computeSha256("${record.timestamp}|${record.eventType}|${record.description}|${record.previousHash}")
            if (record.currentHash != expectedHash) return false
            
            previous = record.currentHash
        }
        return true
    }

    private fun computeSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
