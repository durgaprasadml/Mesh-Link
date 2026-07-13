package com.meshlink.data.security

enum class TrustLevel {
    UNKNOWN,
    DISCOVERED,
    VERIFIED,
    TRUSTED,
    BLOCKED,
    REVOKED
}

enum class VerificationStatus {
    NOT_VERIFIED,
    PENDING,
    VERIFIED
}
