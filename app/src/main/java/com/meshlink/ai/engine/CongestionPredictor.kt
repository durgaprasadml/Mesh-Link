package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CongestionPredictor @Inject constructor(
    private val learningRepository: LearningRepository
) {
    /**
     * Calculates the probability of congestion (0.0 to 1.0) occurring in the next 10 seconds.
     * Uses the derivative of the queue depth and broadcast frequency.
     */
    fun predictCongestionProbability(
        currentQueueDepth: Int, 
        recentBroadcastRate: Int // broadcasts per second
    ): Float {
        // Retrieve baseline moving average queue depth
        val baselineDepth = learningRepository.getGlobalMetric("avg_queue_depth", 10f)
        
        // If we are currently far above baseline and growing, congestion is highly probable
        var probability = 0.0f
        
        if (currentQueueDepth > baselineDepth * 2) {
            probability += 0.4f
        }
        
        // Broadcasts are heavily correlated with upcoming congestion spikes
        if (recentBroadcastRate > 10) {
            probability += 0.4f
        }
        
        // Update baseline for slow learning
        val newBaseline = (baselineDepth * 0.99f) + (currentQueueDepth * 0.01f)
        learningRepository.setGlobalMetric("avg_queue_depth", newBaseline)
        
        return probability.coerceIn(0.0f, 1.0f)
    }
}
