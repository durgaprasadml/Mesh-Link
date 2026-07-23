package com.meshlink.ai.engine

import com.meshlink.routing.engine.CongestionMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshAIManager @Inject constructor(
    val routePredictionEngine: RoutePredictionEngine,
    val congestionPredictor: CongestionPredictor,
    val batteryPredictor: BatteryPredictor,
    val transportPredictor: TransportPredictor,
    val failurePredictor: FailurePredictor,
    val userBehaviorEngine: UserBehaviorEngine,
    val anomalyDetector: AnomalyDetector,
    val recommendationEngine: RecommendationEngine,
    private val congestionMonitor: CongestionMonitor
) {
    
    private val _healthScore = MutableStateFlow(100)
    val healthScore: StateFlow<Int> = _healthScore.asStateFlow()

    private val _activeRecommendations = MutableStateFlow<List<String>>(emptyList())
    val activeRecommendations: StateFlow<List<String>> = _activeRecommendations.asStateFlow()

    /**
     * Periodically called by a background worker to evaluate the global health of the mesh.
     */
    fun evaluateSystemHealth(batteryPct: Int, recentBroadcastRate: Int) {
        var score = 100
        
        val queueDepth = congestionMonitor.getQueueDepth()
        val congestionProb = congestionPredictor.predictCongestionProbability(queueDepth, recentBroadcastRate)
        
        score -= (congestionProb * 40).toInt() // Max 40 point penalty for congestion
        
        if (batteryPct < 20) {
            score -= 20
        }
        
        if (anomalyDetector.isBroadcastRateAnomalous(recentBroadcastRate)) {
            score -= 30
        }
        
        _healthScore.value = score.coerceIn(0, 100)
        
        _activeRecommendations.value = recommendationEngine.generateRecommendations(
            queueDepth, recentBroadcastRate, batteryPct
        )
    }
}
