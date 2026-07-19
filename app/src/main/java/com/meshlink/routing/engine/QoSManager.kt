package com.meshlink.routing.engine

import com.meshlink.domain.model.PacketType
import javax.inject.Inject
import javax.inject.Singleton

enum class QoSPriority(val value: Int) {
    CRITICAL(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1)
}

@Singleton
class QoSManager @Inject constructor() {

    fun getPriority(packetType: PacketType): QoSPriority {
        return when (packetType) {
            PacketType.SOS -> QoSPriority.CRITICAL
            PacketType.KEY_EXCHANGE, PacketType.WIFI_NEGOTIATION, PacketType.SESSION_REKEY -> QoSPriority.HIGH
            PacketType.TEXT, PacketType.DELIVERY_ACK, PacketType.READ_RECEIPT -> QoSPriority.MEDIUM
            PacketType.MEDIA_META, PacketType.MEDIA_CHUNK, PacketType.MEDIA_ACK, PacketType.MEDIA_NACK, PacketType.LOCATION -> QoSPriority.LOW
            else -> QoSPriority.MEDIUM
        }
    }

    /**
     * Used by the engine to determine if a packet should bypass the queue and be processed/sent immediately.
     */
    fun shouldBypassQueue(packetType: PacketType): Boolean {
        return getPriority(packetType) == QoSPriority.CRITICAL
    }
    
    /**
     * Configures the maximum TTL allowed based on priority.
     */
    fun getMaxTtl(packetType: PacketType, defaultMax: Int): Int {
        return if (getPriority(packetType) == QoSPriority.CRITICAL) {
            20 // Max possible for SOS
        } else {
            defaultMax
        }
    }
}
