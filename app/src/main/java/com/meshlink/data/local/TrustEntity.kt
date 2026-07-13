package com.meshlink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trust_table")
data class TrustEntity(
    @PrimaryKey val peerId: String,
    val deviceUUID: String?,
    val fingerprint: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val lastIPAddress: String?,
    val lastBLEAddress: String?,
    val keyVersion: Int,
    val trustLevel: String, // String representation of TrustLevel enum
    val verificationStatus: String, // String representation of VerificationStatus enum
    val trustScore: Int,
    val identityHistory: String // JSON array of past fingerprints/metadata
)
