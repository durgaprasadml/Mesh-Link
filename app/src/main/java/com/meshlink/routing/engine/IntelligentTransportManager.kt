package com.meshlink.routing.engine

import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.wifi.api.HybridTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class IntelligentTransportManager @Inject constructor(
    private val routeOptimizer: RouteOptimizer,
    private val settingsRepository: SettingsRepository,
    private val hybridTransport: HybridTransport,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private var currentPreferredTransport: String = "AUTOMATIC"

    init {
        applicationScope.launch {
            settingsRepository.preferredTransport.collect { mode ->
                currentPreferredTransport = mode
            }
        }
    }

    /**
     * Given a target, payload type, and payload size, determines whether this should go over
     * BLE, Wi-Fi Direct, or Hybrid using HybridTransport decision logic.
     */
    fun selectTransportForPayload(destinationId: String, packetType: PacketType, payloadSizeBytes: Long = 1024L): RouteType {
        return hybridTransport.getSelectedRouteType(destinationId, packetType, payloadSizeBytes)
    }

    fun isHighBandwidthRequired(packetType: PacketType): Boolean {
        return when (packetType) {
            PacketType.VIDEO_FRAME,
            PacketType.VOICE_FRAME,
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_META,
            PacketType.MEDIA_ACK,
            PacketType.MEDIA_NACK -> true
            else -> false
        }
    }
}
