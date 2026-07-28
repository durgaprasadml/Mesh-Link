package com.meshlink.routing.engine


import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.RouteType

@Singleton
class IntelligentTransportManager @Inject constructor(
    private val routeOptimizer: RouteOptimizer,
    private val settingsRepository: SettingsRepository,
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
     * Given a target and the payload type, determines whether this should go over
     * BLE, Wi-Fi Direct, or Hybrid.
     */
    fun selectTransportForPayload(destinationId: String, packetType: PacketType, payloadSizeBytes: Long = 1024L): RouteType {
        // BLE is the only supported transport
        return RouteType.BLE
    }
    
    private fun isHighBandwidthRequired(packetType: PacketType): Boolean {
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
