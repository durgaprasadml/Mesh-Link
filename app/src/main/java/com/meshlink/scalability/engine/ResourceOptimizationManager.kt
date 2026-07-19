package com.meshlink.scalability.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceOptimizationManager @Inject constructor() {

    fun shouldDelayNonCriticalTasks(heapUsedMb: Long, maxHeapMb: Long, batteryPercent: Int): Boolean {
        // If heap is > 85% full, we are risking OOM during a topology shift
        if (heapUsedMb.toFloat() / maxHeapMb > 0.85f) {
            return true
        }
        
        // If battery is < 15%, we suppress heavy graph computations
        if (batteryPercent < 15) {
            return true
        }
        
        return false
    }

    fun optimizeQueue(currentQueueSize: Int): Boolean {
        // Recommend compressing or dropping old packets if queue depth is saturated
        return currentQueueSize > 500
    }
}
