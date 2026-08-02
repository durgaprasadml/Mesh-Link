package com.meshlink.routing.engine

import com.meshlink.ble.api.BleTransport
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshError
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.transport.TransportHealth
import com.meshlink.wifi.api.WifiTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Singleton
class IntelligentTransportManager @Inject constructor(
    private val bleTransport: BleTransport,
    private val wifiTransport: WifiTransport,
    private val routeOptimizer: RouteOptimizer,
    private val settingsRepository: SettingsRepository,
    val metrics: TransportMetrics,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    companion object {
        private const val TAG = "IntelligentTransportManager"
        private const val LARGE_PAYLOAD_THRESHOLD = 50_000L // 50 KB
        private const val RETRY_DELAY_MS = 100L
    }

    private var currentPreferredTransport: String = "AUTOMATIC"

    val bleHealth: StateFlow<TransportHealth> get() = bleTransport.health
    val wifiHealth: StateFlow<TransportHealth> get() = wifiTransport.health

    init {
        applicationScope.launch {
            settingsRepository.preferredTransport.collect { mode ->
                currentPreferredTransport = mode
            }
        }
    }

    fun isWifiAvailable(): Boolean {
        return wifiTransport.connectedPeers.isNotEmpty() || wifiTransport.health.value == TransportHealth.CONNECTED
    }

    fun isBleAvailable(): Boolean {
        return bleTransport.connectedPeers.isNotEmpty() || bleTransport.health.value == TransportHealth.CONNECTED || bleTransport.health.value == TransportHealth.AVAILABLE
    }

    /**
     * Determines whether a packet should go over BLE or Wi-Fi Direct based on packet type,
     * mime type, size, and settings.
     */
    fun selectTransportForPayload(
        destinationId: String,
        packetType: PacketType,
        payloadSizeBytes: Long = 1024L,
        mimeType: String? = null
    ): RouteType {
        if (currentPreferredTransport == "WIFI_DIRECT") return RouteType.WIFI_DIRECT
        if (currentPreferredTransport == "BLE") return RouteType.BLE

        // Explicit Wi-Fi Direct payloads: Images, Audio, Voice, Video, Documents, Files > 50KB, RESOURCE_SYNC
        val isHighBandwidth = isHighBandwidthRequired(packetType, mimeType, payloadSizeBytes)

        return if (isHighBandwidth) {
            RouteType.WIFI_DIRECT
        } else {
            RouteType.BLE
        }
    }

    fun selectTransportForPacket(packet: MeshPacket): RouteType {
        val payloadSize = packet.payload.toByteArray(Charsets.UTF_8).size.toLong()
        return selectTransportForPayload(
            destinationId = packet.targetId,
            packetType = packet.type,
            payloadSizeBytes = payloadSize,
            mimeType = packet.mimeType
        )
    }

    /**
     * Sends/broadcasts a packet through the optimal transport layer with automatic retry & fallback.
     */
    suspend fun sendPacket(
        packet: MeshPacket,
        includeAddress: String? = null,
        excludeAddress: String? = null
    ): MeshResult<Unit> {
        val preferredTransport = selectTransportForPacket(packet)
        val payloadSize = packet.payload.toByteArray(Charsets.UTF_8).size

        return if (preferredTransport == RouteType.WIFI_DIRECT) {
            if (isWifiAvailable()) {
                MeshLogger.d(TAG, "Packet ID=${packet.packetId} Type=${packet.type} Size=${payloadSize}B -> Selected Transport: Wi-Fi Direct (Reason: High-bandwidth / Large Payload)")
                val wifiResult = sendOverWifiWithRetry(packet, includeAddress, excludeAddress)
                if (wifiResult is MeshResult.Success) {
                    metrics.recordWifiPacket(payloadSize)
                    wifiResult
                } else if (payloadSize <= LARGE_PAYLOAD_THRESHOLD) {
                    MeshLogger.w(TAG, "Wi-Fi Direct send failed for packet ${packet.packetId} (${payloadSize}B <= 50KB). Fallback -> BLE")
                    metrics.recordFallback()
                    val fallbackBleResult = sendOverBleWithRetry(packet, includeAddress, excludeAddress)
                    if (fallbackBleResult is MeshResult.Success) {
                        metrics.recordBlePacket(payloadSize)
                    }
                    fallbackBleResult
                } else {
                    MeshLogger.w(TAG, "Wi-Fi Direct send failed for large packet ${packet.packetId} (${payloadSize}B > 50KB). Skipping BLE fallback to preserve BLE bandwidth.")
                    wifiResult
                }
            } else if (payloadSize <= LARGE_PAYLOAD_THRESHOLD) {
                MeshLogger.d(TAG, "Packet ID=${packet.packetId} Type=${packet.type} -> Preferred Wi-Fi Direct unavailable. Small payload (${payloadSize}B <= 50KB), Fallback -> BLE")
                metrics.recordFallback()
                val fallbackBleResult = sendOverBleWithRetry(packet, includeAddress, excludeAddress)
                if (fallbackBleResult is MeshResult.Success) {
                    metrics.recordBlePacket(payloadSize)
                }
                fallbackBleResult
            } else {
                MeshLogger.w(TAG, "Wi-Fi Direct unavailable for large packet ${packet.packetId} (${payloadSize}B > 50KB). Staying queued for Wi-Fi Direct.")
                MeshResult.Error(MeshError.TransportError("Wi-Fi Direct unavailable for large payload (${payloadSize}B)"))
            }
        } else {
            MeshLogger.d(TAG, "Packet ID=${packet.packetId} Type=${packet.type} Size=${payloadSize}B -> Selected Transport: BLE (Reason: Lightweight / Signaling)")
            val bleResult = sendOverBleWithRetry(packet, includeAddress, excludeAddress)
            if (bleResult is MeshResult.Success) {
                metrics.recordBlePacket(payloadSize)
                bleResult
            } else if (isWifiAvailable()) {
                MeshLogger.w(TAG, "BLE send failed for packet ${packet.packetId}. Fallback attempt -> Wi-Fi Direct")
                metrics.recordFallback()
                val fallbackWifiResult = sendOverWifiWithRetry(packet, includeAddress, excludeAddress)
                if (fallbackWifiResult is MeshResult.Success) {
                    metrics.recordWifiPacket(payloadSize)
                    fallbackWifiResult
                } else {
                    bleResult
                }
            } else {
                bleResult
            }
        }
    }

    private suspend fun sendOverWifiWithRetry(
        packet: MeshPacket,
        includeAddress: String?,
        excludeAddress: String?
    ): MeshResult<Unit> {
        var result = wifiTransport.broadcastPacket(packet, excludeAddress = excludeAddress, includeAddress = includeAddress)
        if (result is MeshResult.Success) return result

        MeshLogger.w(TAG, "Retrying Wi-Fi Direct send for packet ${packet.packetId}...")
        metrics.recordRetry()
        delay(RETRY_DELAY_MS)
        result = wifiTransport.broadcastPacket(packet, excludeAddress = excludeAddress, includeAddress = includeAddress)
        return result
    }

    private suspend fun sendOverBleWithRetry(
        packet: MeshPacket,
        includeAddress: String?,
        excludeAddress: String?
    ): MeshResult<Unit> {
        var result = bleTransport.broadcastPacket(packet, excludeAddress = excludeAddress, includeAddress = includeAddress)
        if (result is MeshResult.Success) return result

        MeshLogger.w(TAG, "Retrying BLE send for packet ${packet.packetId}...")
        metrics.recordRetry()
        delay(RETRY_DELAY_MS)
        result = bleTransport.broadcastPacket(packet, excludeAddress = excludeAddress, includeAddress = includeAddress)
        return result
    }

    private fun isHighBandwidthRequired(
        packetType: PacketType,
        mimeType: String?,
        payloadSizeBytes: Long
    ): Boolean {
        if (payloadSizeBytes > LARGE_PAYLOAD_THRESHOLD) return true

        if (mimeType != null) {
            val lower = mimeType.lowercase()
            if (lower.startsWith("image/") ||
                lower.startsWith("video/") ||
                lower.startsWith("audio/") ||
                lower.contains("pdf") ||
                lower.contains("zip") ||
                lower.contains("apk") ||
                lower.contains("application/")
            ) {
                return true
            }
        }

        return when (packetType) {
            PacketType.VOICE_FRAME,
            PacketType.VIDEO_FRAME,
            PacketType.RESOURCE_SYNC -> true
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_META -> {
                // If mimeType is unknown, treat chunks/meta as high-bandwidth by default
                true
            }
            else -> false
        }
    }
}
