package com.meshlink.routing.engine

import com.meshlink.ai.engine.CongestionPredictor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

enum class CongestionLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Singleton
class CongestionMonitor @Inject constructor(
    private val congestionPredictor: CongestionPredictor
) {

    private val localPendingQueue = AtomicInteger(0)
    private val localRelayQueue = AtomicInteger(0)
    
    // Broadcast Storm tracking
    private val broadcastsInWindow = AtomicInteger(0)
    private var lastWindowTime = System.currentTimeMillis()
    private val BROADCAST_WINDOW_MS = 5000L
    private val BROADCAST_STORM_THRESHOLD = 50 // >50 broadcasts per 5 seconds is a storm

    private val _congestionLevel = MutableStateFlow(CongestionLevel.LOW)
    val congestionLevel: StateFlow<CongestionLevel> = _congestionLevel.asStateFlow()

    private var currentCongestionScore = 0 // 0 - 100

    fun incrementPending() {
        localPendingQueue.incrementAndGet()
        evaluateCongestion()
    }

    fun decrementPending() {
        if (localPendingQueue.decrementAndGet() < 0) localPendingQueue.set(0)
        evaluateCongestion()
    }
    
    fun incrementRelay() {
        localRelayQueue.incrementAndGet()
        evaluateCongestion()
    }

    fun decrementRelay() {
        if (localRelayQueue.decrementAndGet() < 0) localRelayQueue.set(0)
        evaluateCongestion()
    }

    fun recordBroadcast() {
        val now = System.currentTimeMillis()
        if (now - lastWindowTime > BROADCAST_WINDOW_MS) {
            broadcastsInWindow.set(0)
            lastWindowTime = now
        }
        broadcastsInWindow.incrementAndGet()
        evaluateCongestion()
    }

    fun isBroadcastStorm(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastWindowTime > BROADCAST_WINDOW_MS) {
            broadcastsInWindow.set(0)
            lastWindowTime = now
            return false
        }
        return broadcastsInWindow.get() > BROADCAST_STORM_THRESHOLD
    }
    
    fun getQueueDepth(): Int {
        return localPendingQueue.get() + localRelayQueue.get()
    }

    fun getCongestionScore(): Int {
        return currentCongestionScore
    }

    private fun evaluateCongestion() {
        val totalDepth = getQueueDepth()
        
        // Base score off queue depth (max 300 = 100 score)
        var score = (totalDepth / 300f) * 100f
        
        // Add storm penalty
        if (isBroadcastStorm()) {
            score += 50f
        }
        
        // Add predictive penalty
        val broadcastsPerSec = broadcastsInWindow.get() / Math.max(1L, (System.currentTimeMillis() - lastWindowTime) / 1000L).toInt()
        val congestionProb = congestionPredictor.predictCongestionProbability(totalDepth, broadcastsPerSec)
        
        score += (congestionProb * 30f) // Can add up to 30 points if congestion is predicted to rise

        currentCongestionScore = Math.min(100, score.toInt())

        val newLevel = when {
            currentCongestionScore < 30 -> CongestionLevel.LOW
            currentCongestionScore < 60 -> CongestionLevel.MEDIUM
            currentCongestionScore < 85 -> CongestionLevel.HIGH
            else -> CongestionLevel.CRITICAL
        }
        
        if (_congestionLevel.value != newLevel) {
            _congestionLevel.value = newLevel
        }
    }
    
    fun isCongested(): Boolean {
        return _congestionLevel.value == CongestionLevel.HIGH || _congestionLevel.value == CongestionLevel.CRITICAL
    }
}
