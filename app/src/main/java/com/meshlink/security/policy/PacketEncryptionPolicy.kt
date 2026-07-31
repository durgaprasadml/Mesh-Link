package com.meshlink.security.policy

import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.MeshPacket

enum class EncryptionRequirement {
    REQUIRED,
    OPTIONAL,
    BOOTSTRAP_ONLY
}

object PacketEncryptionPolicy {

    /**
     * Determines whether the given packet type must be encrypted.
     */
    fun getRequirement(type: PacketType): EncryptionRequirement {
        return when (type) {
            PacketType.KEY_EXCHANGE,
            PacketType.BEACON,
            PacketType.ROUTE_REQUEST,
            PacketType.ROUTE_REPLY,
            PacketType.ROUTE_ERROR -> EncryptionRequirement.BOOTSTRAP_ONLY
            
            PacketType.SESSION_REKEY -> EncryptionRequirement.OPTIONAL
            
            else -> EncryptionRequirement.REQUIRED
        }
    }

    /**
     * Validates whether a packet's encryption state is acceptable based on centralized policy.
     * All user payloads (text, media, location, SOS, broadcast) must be encrypted.
     */
    fun validatePacketEncryption(
        packet: MeshPacket,
        strictMode: Boolean,
        hasSecureSession: Boolean
    ): Boolean {
        // If the packet is encrypted, it is accepted policy-wise (decryption happens subsequently).
        if (packet.encrypted) return true

        return when (getRequirement(packet.type)) {
            EncryptionRequirement.REQUIRED -> {
                // Must always be encrypted. Plaintext user payload is rejected.
                false
            }
            EncryptionRequirement.OPTIONAL -> {
                !strictMode && !hasSecureSession
            }
            EncryptionRequirement.BOOTSTRAP_ONLY -> {
                // Routing control & key exchange bootstrapping allowed in plaintext
                true
            }
        }
    }
}
