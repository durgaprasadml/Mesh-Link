package com.meshlink.securitytest

import com.meshlink.simulator.node.SimulatedNode.NodeConfig

object SecurityTestFixtures {

    const val DEFAULT_SEED = 42L
    const val DEFAULT_TTL = 32
    const val STANDARD_PAYLOAD = "CONFIDENTIAL_PAYLOAD_001"
    const val ALICE_ID = "Alice"
    const val BOB_ID = "Bob"
    const val EVE_ID = "Eve"

    /**
     * Strict security configuration to prevent legacy downgrade.
     */
    fun strictSecurityConfig(): NodeConfig = NodeConfig(
        defaultTtl = DEFAULT_TTL,
        maxHops = DEFAULT_TTL,
        enforceEncryption = true
    )
    
    /**
     * Permissive security configuration (for Eve).
     */
    fun attackerConfig(): NodeConfig = NodeConfig(
        defaultTtl = DEFAULT_TTL,
        maxHops = DEFAULT_TTL,
        enforceEncryption = false // Eve doesn't care
    )
}
