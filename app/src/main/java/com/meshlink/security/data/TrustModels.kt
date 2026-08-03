package com.meshlink.security.data

typealias TrustLevel = com.meshlink.trust.TrustLevel

enum class VerificationStatus {
    NOT_VERIFIED,
    PENDING,
    VERIFIED
}
