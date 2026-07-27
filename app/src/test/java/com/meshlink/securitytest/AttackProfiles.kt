package com.meshlink.securitytest

/**
 * Defines reusable attack profiles that dictate how the attacker node manipulates packets.
 */
sealed class AttackProfile {
    object ReplayAttack : AttackProfile()
    object MITM : AttackProfile()
    object Downgrade : AttackProfile()
    object Tampering : AttackProfile()
    object IdentitySpoofing : AttackProfile()
    object MalformedPackets : AttackProfile()
    object SessionExhaustion : AttackProfile()
    object PassThrough : AttackProfile() // Benign forwarding
}
