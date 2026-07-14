package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import com.meshlink.routing.engine.RouteType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportPredictor @Inject constructor(
    private val learningRepository: LearningRepository,
    private val batteryPredictor: BatteryPredictor
) {
    /**
     * Predicts whether Wi-Fi or BLE will be best for a specific payload size and peer.
     * Takes into account historical success rates of the transport with that peer.
     */
    fun predictBestTransport(peerId: String, payloadSizeBytes: Long): RouteType {
        val metrics = learningRepository.getMetricsForPeer(peerId)
        
        val wifiSuccesses = metrics.successfulWifiConnections
        val bleSuccesses = metrics.successfulBleConnections

        // Very small payloads always favor BLE for power efficiency, unless BLE is completely broken
        if (payloadSizeBytes < 1024 * 50) { // < 50KB
            if (bleSuccesses > 0 || wifiSuccesses == 0L) {
                return RouteType.BLE
            }
        }
        
        // Large payloads favor Wi-Fi Direct
        // Compare battery cost
        val wifiCost = batteryPredictor.predictEnergyCost(payloadSizeBytes, true)
        val bleCost = batteryPredictor.predictEnergyCost(payloadSizeBytes, false)
        
        // If Wi-Fi is much cheaper (which it usually is for bulk data), and it has succeeded before, prefer it.
        if (wifiCost < bleCost * 0.5f && wifiSuccesses >= bleSuccesses * 0.1f) {
            return RouteType.WIFI_DIRECT
        }
        
        // Default to BLE if history says Wi-Fi Direct is highly unstable for this peer
        return RouteType.BLE
    }
}
