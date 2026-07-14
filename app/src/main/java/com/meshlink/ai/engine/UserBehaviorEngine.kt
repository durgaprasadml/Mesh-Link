package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserBehaviorEngine @Inject constructor(
    private val learningRepository: LearningRepository
) {
    /**
     * Analyzes historical transfers to determine average payload sizes to particular peers.
     */
    fun getTypicalTransferSize(peerId: String): Long {
        val metrics = learningRepository.getMetricsForPeer(peerId)
        if (metrics.totalPacketsDelivered == 0L) return 1024L // Default 1KB
        
        return metrics.totalBytesTransferred / metrics.totalPacketsDelivered
    }
    
    /**
     * Recommends whether we should aggressively keep a connection alive with this peer.
     */
    fun isFrequentContact(peerId: String): Boolean {
        val metrics = learningRepository.getMetricsForPeer(peerId)
        return metrics.totalPacketsSent > 1000 // Arbitrary threshold for "frequent"
    }
}
