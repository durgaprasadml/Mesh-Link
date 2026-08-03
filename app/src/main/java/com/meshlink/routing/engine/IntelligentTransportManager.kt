package com.meshlink.routing.engine

import com.meshlink.ble.api.BleTransport
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

/**
 * Intelligent transport manager orchestrating media and control traffic separation,
 * transport health monitoring, metrics tracking, policy enforcement, and diagnostics.
 */
@Singleton
class IntelligentTransportManager @Inject constructor(
    private val bleTransport: BleTransport,
    private val wifiTransport: WifiTransport,
    private val routeOptimizer: RouteOptimizer,
    private val settingsRepository: SettingsRepository,
    val metrics: TransportMetrics,
    @ApplicationScope private val applicationScope: CoroutineScope,
    val classifier: TransportPacketClassifier = TransportPacketClassifier(),
    val policy: TransportPolicy = TransportPolicy(classifier),
    val healthMonitor: TransportHealthMonitor = TransportHealthMonitor(),
    val diagnostics: TransportDiagnostics = TransportDiagnostics(),
    val queueManager: TransportQueueManager? = null
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
     * Determines whether a packet should go over BLE or Wi-Fi Direct based on classification policies.
     */
    fun selectTransportForPayload(
        destinationId: String,
        packetType: PacketType,
        payloadSizeBytes: Long = 1024L,
        mimeType: String? = null
    ): RouteType {
        if (currentPreferredTransport == "WIFI_DIRECT") return RouteType.WIFI_DIRECT
        if (currentPreferredTransport == "BLE") return RouteType.BLE

        return policy.getPreferredTransport(packetType, payloadSizeBytes, mimeType)
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
     * Sends/broadcasts a packet through the optimal transport layer with automatic retry & policy-driven fallback.
     */
    suspend fun sendPacket(
        packet: MeshPacket,
        includeAddress: String? = null,
        excludeAddress: String? = null
    ): MeshResult<Unit> {
        val category = classifier.classify(packet)
        val preferredTransport = selectTransportForPacket(packet)
        val payloadSize = packet.payload.toByteArray(Charsets.UTF_8).size.toLong()

        diagnostics.logTransportSelection(
            packetId = packet.packetId,
            packetType = packet.type,
            category = category,
            selectedRoute = preferredTransport,
            reason = "Classified as $category traffic (size=${payloadSize}B)"
        )

        return if (preferredTransport == RouteType.WIFI_DIRECT) {
            if (isWifiAvailable()) {
                val wifiResult = sendOverWifiWithRetry(packet, includeAddress, excludeAddress)
                if (wifiResult is MeshResult.Success) {
                    metrics.recordWifiPacket(payloadSize.toInt())
                    healthMonitor.recordWifiTxResult(true, bytes = payloadSize.toInt())
                    wifiResult
                } else if (policy.shouldAllowBleFallback(packet, category)) {
                    diagnostics.logTransportFallback(
                        packetId = packet.packetId,
                        packetType = packet.type,
                        primaryRoute = RouteType.WIFI_DIRECT,
                        fallbackRoute = RouteType.BLE,
                        reason = "Wi-Fi Direct failed for small payload (${payloadSize}B)"
                    )
                    metrics.recordFallback()
                    healthMonitor.recordFallback()
                    val fallbackBleResult = sendOverBleWithRetry(packet, includeAddress, excludeAddress)
                    if (fallbackBleResult is MeshResult.Success) {
                        metrics.recordBlePacket(payloadSize.toInt())
                        healthMonitor.recordBleTxResult(true)
                    }
                    fallbackBleResult
                } else {
                    diagnostics.logTransportUnavailable(
                        packetId = packet.packetId,
                        packetType = packet.type,
                        requestedRoute = RouteType.WIFI_DIRECT,
                        reason = "Wi-Fi Direct failed for large media payload (${payloadSize}B). Skipping BLE fallback."
                    )
                    healthMonitor.recordWifiTxResult(false)
                    wifiResult
                }
            } else if (policy.shouldAllowBleFallback(packet, category)) {
                diagnostics.logTransportFallback(
                    packetId = packet.packetId,
                    packetType = packet.type,
                    primaryRoute = RouteType.WIFI_DIRECT,
                    fallbackRoute = RouteType.BLE,
                    reason = "Wi-Fi Direct unavailable for small payload (${payloadSize}B)"
                )
                metrics.recordFallback()
                healthMonitor.recordFallback()
                val fallbackBleResult = sendOverBleWithRetry(packet, includeAddress, excludeAddress)
                if (fallbackBleResult is MeshResult.Success) {
                    metrics.recordBlePacket(payloadSize.toInt())
                    healthMonitor.recordBleTxResult(true)
                }
                fallbackBleResult
            } else {
                diagnostics.logTransportUnavailable(
                    packetId = packet.packetId,
                    packetType = packet.type,
                    requestedRoute = RouteType.WIFI_DIRECT,
                    reason = "Wi-Fi Direct unavailable for payload type ${packet.type} (${payloadSize}B). Remaining queued."
                )
                healthMonitor.recordWifiTxResult(false)
                MeshResult.Error(MeshError.TransportError("Wi-Fi Direct unavailable for payload type ${packet.type} (${payloadSize}B)"))
            }
        } else {
            val bleResult = sendOverBleWithRetry(packet, includeAddress, excludeAddress)
            if (bleResult is MeshResult.Success) {
                metrics.recordBlePacket(payloadSize.toInt())
                healthMonitor.recordBleTxResult(true)
                bleResult
            } else if (isWifiAvailable()) {
                diagnostics.logTransportFallback(
                    packetId = packet.packetId,
                    packetType = packet.type,
                    primaryRoute = RouteType.BLE,
                    fallbackRoute = RouteType.WIFI_DIRECT,
                    reason = "BLE send failed. Fallback attempt over Wi-Fi Direct."
                )
                metrics.recordFallback()
                healthMonitor.recordFallback()
                val fallbackWifiResult = sendOverWifiWithRetry(packet, includeAddress, excludeAddress)
                if (fallbackWifiResult is MeshResult.Success) {
                    metrics.recordWifiPacket(payloadSize.toInt())
                    healthMonitor.recordWifiTxResult(true, bytes = payloadSize.toInt())
                    fallbackWifiResult
                } else {
                    healthMonitor.recordBleTxResult(false)
                    bleResult
                }
            } else {
                healthMonitor.recordBleTxResult(false)
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

        diagnostics.logRetry(
            packetId = packet.packetId,
            packetType = packet.type,
            attempt = 1,
            transport = RouteType.WIFI_DIRECT,
            reason = "Retrying Wi-Fi Direct send..."
        )
        metrics.recordWifiRetry()
        healthMonitor.recordRetry()
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

        diagnostics.logRetry(
            packetId = packet.packetId,
            packetType = packet.type,
            attempt = 1,
            transport = RouteType.BLE,
            reason = "Retrying BLE send..."
        )
        metrics.recordBleRetry()
        healthMonitor.recordRetry()
        delay(RETRY_DELAY_MS)
        result = bleTransport.broadcastPacket(packet, excludeAddress = excludeAddress, includeAddress = includeAddress)
        return result
    }
}
