package com.meshlink.routing.engine

import com.meshlink.ai.engine.TransportPredictor
import com.meshlink.ble.data.PacketType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntelligentTransportManager @Inject constructor(
    private val routeOptimizer: RouteOptimizer,
    private val transportPredictor: TransportPredictor
) {

    /**
     * Given a target and the payload type, determines whether this should go over
     * BLE, Wi-Fi Direct, or Hybrid.
     */
    fun selectTransportForPayload(destinationId: String, packetType: PacketType, payloadSizeBytes: Long = 1024L): RouteType {
        // Consult AI Transport Predictor first
        val predictedBest = transportPredictor.predictBestTransport(destinationId, payloadSizeBytes)
        
        // Find best known route to the destination
        val bestRoute = routeOptimizer.getOptimalRoute(destinationId)
        
        if (bestRoute == null) {
            // No directed route known, fallback to predicted best (typically BLE broadcast)
            return predictedBest
        }

        // Heavy payloads require high bandwidth
        if (isHighBandwidthRequired(packetType)) {
            // Force Wi-Fi Direct if available
            return if (bestRoute.routeType == RouteType.WIFI_DIRECT || bestRoute.routeType == RouteType.HYBRID) {
                bestRoute.routeType
            } else {
                RouteType.BLE
            }
        }
        
        // Prefer the AI prediction if the route supports it (e.g. if hybrid is available we can pick either)
        if (bestRoute.routeType == RouteType.HYBRID) {
            return predictedBest
        }
        
        return bestRoute.routeType
    }
    
    private fun isHighBandwidthRequired(packetType: PacketType): Boolean {
        return when (packetType) {
            PacketType.VIDEO_FRAME, 
            PacketType.VOICE, 
            PacketType.FILE_CHUNK -> true
            else -> false
        }
    }
}
