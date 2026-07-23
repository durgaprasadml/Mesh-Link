package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnomalyDetector @Inject constructor(
    private val learningRepository: LearningRepository
) {
    companion object {
        private const val TAG = "AnomalyDetector"
    }

    /**
     * Checks if the current broadcast rate is vastly abnormal compared to the historical baseline.
     */
    fun isBroadcastRateAnomalous(currentRate: Int): Boolean {
        val baseline = learningRepository.getGlobalMetric("avg_broadcast_rate", 2f)
        
        // If current rate is 10x the baseline and > 20, it's likely a broadcast storm / flood attack
        if (currentRate > 20 && currentRate > baseline * 10) {
            MeshLogger.w(TAG, "ANOMALY DETECTED: Broadcast rate $currentRate is >10x baseline ($baseline)!")
            return true
        }
        
        // Slowly learn new baseline
        val newBaseline = (baseline * 0.999f) + (currentRate * 0.001f)
        learningRepository.setGlobalMetric("avg_broadcast_rate", newBaseline)
        
        return false
    }

    /**
     * Checks if a peer is sending an abnormal amount of packets (potential DDoS).
     */
    fun isPeerTrafficAnomalous(peerId: String, currentWindowPackets: Int): Boolean {
        val metrics = learningRepository.getMetricsForPeer(peerId)
        val avgTraffic = if (metrics.totalPacketsSent > 0) metrics.totalPacketsSent / 1000f else 1f // rough estimate
        
        if (currentWindowPackets > 50 && currentWindowPackets > avgTraffic * 20) {
            MeshLogger.w(TAG, "ANOMALY DETECTED: Peer ${peerId.takeLast(6)} sending suspicious volume of traffic ($currentWindowPackets packets in window).")
            return true
        }
        return false
    }
}
