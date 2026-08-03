package com.meshlink.trust

/**
 * Verified identity trust levels for Mesh-Link network nodes.
 * Represents confidence in peer identity verification.
 */
enum class TrustLevel(val rank: Int) {
    UNKNOWN(0),
    DISCOVERED(1),
    KNOWN(2),
    VERIFIED(3),
    ADMIN(4),
    EMERGENCY_AUTHORITY(5),
    TRUSTED(4),
    BLOCKED(-1),
    REVOKED(-2);

    fun isAtLeast(required: TrustLevel): Boolean {
        if (this == BLOCKED || this == REVOKED) return false
        if (required == BLOCKED || required == REVOKED) return this == required
        return this.rank >= required.rank
    }

    fun isTrusted(): Boolean {
        return this == VERIFIED || this == TRUSTED || this == ADMIN || this == EMERGENCY_AUTHORITY
    }

    fun isVerified(): Boolean {
        return this == VERIFIED || this == TRUSTED || this == ADMIN || this == EMERGENCY_AUTHORITY
    }

    fun isBlockedOrRevoked(): Boolean {
        return this == BLOCKED || this == REVOKED
    }
}
