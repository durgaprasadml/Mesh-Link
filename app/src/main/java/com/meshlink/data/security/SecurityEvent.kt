package com.meshlink.data.security

sealed class SecurityEvent(
    val severity: Int // 1 to 5, where 5 is critical
) {
    data class IdentityChanged(val oldFingerprint: String, val newFingerprint: String) : SecurityEvent(5)
    data class ReplayAttackDetected(val packetId: String) : SecurityEvent(4)
    data class InvalidSignature(val reason: String) : SecurityEvent(4)
    data class SessionHijackAttempt(val details: String) : SecurityEvent(5)
    data class DuplicateFingerprint(val peerId1: String, val peerId2: String) : SecurityEvent(4)
    object UnknownPeer : SecurityEvent(1)
    data class TrustRevoked(val reason: String) : SecurityEvent(3)
    data class BlockedPeer(val peerId: String) : SecurityEvent(2)
}
