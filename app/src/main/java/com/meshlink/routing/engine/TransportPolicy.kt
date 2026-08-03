package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralizes transport selection rules, payload thresholds, fallback criteria,
 * and retry policies across Mesh-Link.
 */
@Singleton
class TransportPolicy @Inject constructor(
    private val classifier: TransportPacketClassifier
) {

    companion object {
        const val BLE_MAX_PAYLOAD_THRESHOLD = 50_000L // 50 KB
        const val DEFAULT_MAX_RETRIES = 2
        const val RETRY_DELAY_MS = 100L
    }

    /**
     * Returns the preferred route type for a given [TransportCategory].
     */
    fun getPreferredTransport(category: TransportCategory): RouteType {
        return when (category) {
            TransportCategory.CONTROL -> RouteType.BLE
            TransportCategory.MEDIA -> RouteType.WIFI_DIRECT
        }
    }

    /**
     * Returns the preferred route type based on packet attributes.
     */
    fun getPreferredTransport(
        packetType: PacketType,
        payloadSizeBytes: Long = 1024L,
        mimeType: String? = null
    ): RouteType {
        val category = classifier.classify(packetType, payloadSizeBytes, mimeType)
        return getPreferredTransport(category)
    }

    /**
     * Determines if a payload qualifies as a media packet.
     */
    fun isMediaPacket(
        packetType: PacketType,
        mimeType: String? = null,
        payloadSizeBytes: Long = 1024L
    ): Boolean {
        return classifier.classify(packetType, payloadSizeBytes, mimeType) == TransportCategory.MEDIA
    }

    /**
     * Evaluates whether fallback to BLE is permitted if Wi-Fi Direct is unavailable.
     *
     * Rule:
     * - Small packets (<=50KB) are allowed to fallback to BLE to ensure connectivity.
     * - Large media payloads (>50KB) MUST NOT flood BLE.
     */
    fun shouldAllowBleFallback(packet: MeshPacket, category: TransportCategory): Boolean {
        val payloadSizeBytes = packet.payload.toByteArray(Charsets.UTF_8).size.toLong()
        return shouldAllowBleFallback(
            packetType = packet.type,
            payloadSizeBytes = payloadSizeBytes,
            category = category
        )
    }

    fun shouldAllowBleFallback(
        packetType: PacketType,
        payloadSizeBytes: Long,
        category: TransportCategory = classifier.classify(packetType, payloadSizeBytes, null)
    ): Boolean {
        return payloadSizeBytes <= BLE_MAX_PAYLOAD_THRESHOLD
    }

    fun getBlePayloadThreshold(): Long = BLE_MAX_PAYLOAD_THRESHOLD

    fun getMaxRetries(): Int = DEFAULT_MAX_RETRIES

    fun getRetryDelayMs(): Long = RETRY_DELAY_MS
}
