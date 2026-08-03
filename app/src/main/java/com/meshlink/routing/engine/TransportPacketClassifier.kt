package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Categorizes network traffic to enable intelligent transport separation.
 *
 * CONTROL traffic (signaling, text, ACKs, presence, routing) is routed via lightweight, low-latency transports (BLE).
 * MEDIA traffic (images, videos, audio, file chunks, large payloads) is routed via high-throughput transports (Wi-Fi Direct).
 */
enum class TransportCategory {
    CONTROL,
    MEDIA
}

@Singleton
class TransportPacketClassifier @Inject constructor() {

    companion object {
        const val DEFAULT_LARGE_PAYLOAD_THRESHOLD = 50_000L // 50 KB
    }

    /**
     * Classifies a full [MeshPacket] into a [TransportCategory].
     */
    fun classify(packet: MeshPacket): TransportCategory {
        val payloadSizeBytes = packet.payload.toByteArray(Charsets.UTF_8).size.toLong()
        return classify(
            packetType = packet.type,
            payloadSizeBytes = payloadSizeBytes,
            mimeType = packet.mimeType
        )
    }

    /**
     * Classifies traffic based on packet type, payload size, and MIME type.
     */
    fun classify(
        packetType: PacketType,
        payloadSizeBytes: Long = 1024L,
        mimeType: String? = null
    ): TransportCategory {
        // 1. Primary Check: Large Payloads > 50KB are always MEDIA
        if (payloadSizeBytes > DEFAULT_LARGE_PAYLOAD_THRESHOLD) {
            return TransportCategory.MEDIA
        }

        // 2. Secondary Check: Media MIME Types
        if (mimeType != null) {
            val lowerMime = mimeType.lowercase()
            if (lowerMime.startsWith("image/") ||
                lowerMime.startsWith("video/") ||
                lowerMime.startsWith("audio/") ||
                lowerMime.contains("pdf") ||
                lowerMime.contains("zip") ||
                lowerMime.contains("apk") ||
                lowerMime.startsWith("application/")
            ) {
                return TransportCategory.MEDIA
            }
        }

        // 3. PacketType Classification
        return when (packetType) {
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_META,
            PacketType.VOICE_FRAME,
            PacketType.VIDEO_FRAME,
            PacketType.RESOURCE_SYNC,
            PacketType.MAP_SYNC -> TransportCategory.MEDIA

            else -> TransportCategory.CONTROL
        }
    }
}
