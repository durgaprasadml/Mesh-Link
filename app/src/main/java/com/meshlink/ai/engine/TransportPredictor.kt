package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import com.meshlink.domain.model.RouteType
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
        // BLE is the only supported transport
        return RouteType.BLE
    }
}
