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
            PacketType.SOS,
            PacketType.BEACON,
            PacketType.PROFILE_IMAGE_REQUEST,
            PacketType.PROFILE_IMAGE_RESPONSE -> EncryptionRequirement.BOOTSTRAP_ONLY
            
            PacketType.SESSION_REKEY -> EncryptionRequirement.OPTIONAL
            
            PacketType.TEXT,
            PacketType.LOCATION,
            PacketType.MEDIA_META,
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_ACK,
            PacketType.MEDIA_NACK,
            PacketType.VOICE_SIGNAL,
            PacketType.VOICE_FRAME,
            PacketType.VIDEO_SIGNAL,
            PacketType.VIDEO_FRAME,
            PacketType.DELIVERY_ACK,
            PacketType.READ_RECEIPT,
            PacketType.INCIDENT_REPORT,
            PacketType.CHECK_IN,
            PacketType.FORM_SYNC,
            PacketType.RESOURCE_SYNC,
            PacketType.MAP_SYNC -> EncryptionRequirement.REQUIRED
        }
    }

    /**
     * Validates whether a packet's encryption state is acceptable based on the centralized policy.
     * 
     * @param packet The incoming packet.
     * @param strictMode True if the network is enforcing strict encryption mode.
     * @param hasSecureSession True if an active secure session exists with the sender.
     */
    fun validatePacketEncryption(
        packet: MeshPacket,
        strictMode: Boolean,
        hasSecureSession: Boolean
    ): Boolean {
        // If the packet is encrypted, it is always allowed policy-wise 
        // (decryption failure will be handled elsewhere).
        if (packet.encrypted) return true

        val isPublicBroadcast = packet.type == PacketType.TEXT &&
            packet.targetId.equals("BROADCAST", ignoreCase = true)
        if (isPublicBroadcast) return true

        return when (getRequirement(packet.type)) {
            EncryptionRequirement.REQUIRED -> {
                // Must always be encrypted. Plaintext is rejected.
                false
            }
            EncryptionRequirement.OPTIONAL -> {
                // Allowed in plaintext only if not in strict mode AND no secure session exists.
                !strictMode && !hasSecureSession
            }
            EncryptionRequirement.BOOTSTRAP_ONLY -> {
                // Exists for bootstrapping; may be transmitted in plaintext.
                true
            }
        }
    }
}
