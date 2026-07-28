package com.meshlink.security.policy

import com.meshlink.domain.model.PacketType

enum class EncryptionRequirement {
    REQUIRED,
    OPTIONAL,
    NONE
}

object PacketEncryptionPolicy {

    /**
     * Determines whether the given packet type must be encrypted.
     * 
     * - Protocol packets (SESSION_REKEY, DELIVERY_ACK, READ_RECEIPT) are always encrypted.
     * - User messages and media metadata are optional based on user settings, but may be 
     *   dropped by receiver if strict enforcement is enabled.
     * - KEY_EXCHANGE, SOS, WIFI_NEGOTIATION are sent in plaintext.
     */
    fun getRequirement(type: PacketType, isDirectDelivery: Boolean = false): EncryptionRequirement {
        return when (type) {
            PacketType.SESSION_REKEY,
            PacketType.DELIVERY_ACK,
            PacketType.READ_RECEIPT -> EncryptionRequirement.REQUIRED
            
            PacketType.TEXT,
            PacketType.LOCATION,
            PacketType.VOICE_SIGNAL,
            PacketType.VOICE_FRAME,
            PacketType.VIDEO_SIGNAL,
            PacketType.VIDEO_FRAME -> EncryptionRequirement.OPTIONAL

            PacketType.MEDIA_META,
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_ACK,
            PacketType.MEDIA_NACK -> {
                if (isDirectDelivery) EncryptionRequirement.OPTIONAL else EncryptionRequirement.REQUIRED
            }

            PacketType.KEY_EXCHANGE,
            PacketType.SOS,
            PacketType.WIFI_NEGOTIATION,
            PacketType.BEACON,
            PacketType.INCIDENT_REPORT,
            PacketType.CHECK_IN,
            PacketType.FORM_SYNC,
            PacketType.RESOURCE_SYNC,
            PacketType.MAP_SYNC -> EncryptionRequirement.NONE
        }
    }
}
