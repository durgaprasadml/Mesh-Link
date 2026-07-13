package com.meshlink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log_table")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val peerId: String,
    val eventName: String,
    val severity: Int,
    val details: String, // Encrypted/stringified details
    val actionTaken: String
)
