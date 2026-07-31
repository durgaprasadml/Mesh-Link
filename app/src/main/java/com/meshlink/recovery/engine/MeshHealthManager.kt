package com.meshlink.recovery.engine

import com.meshlink.domain.model.RouteType
import com.meshlink.routing.engine.CongestionLevel
import com.meshlink.transport.TransportHealthMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class NodeConfidence(
    val nodeId: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val lastSeenMs: Long,
    val batteryLevel: Int,
    val isReachable: Boolean
)

data class RouteConfidence(
    val destinationId: String,
    val nextHop: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val rssi: Int,
    val averageLatencyMs: Long,
    val packetLossRate: Float,
    val hops: Int
)

data class MeshHealthMetrics(
    val meshSize: Int = 0,
    val connectedPeersCount: Int = 0,
    val healthyRoutesCount: Int = 0,
    val failedRoutesCount: Int = 0,
    val congestionLevel: CongestionLevel = CongestionLevel.LOW,
    val transportHealthMap: Map<RouteType, TransportHealthMetrics> = emptyMap(),
    val averageRttMs: Long = 0L,
    val packetLossRate: Float = 0.0f,
    val totalRetries: Long = 0L,
    val totalRepairs: Int = 0,
    val discoveryCount: Int = 0,
    val pendingQueueSize: Int = 0,
    val partitionEvents: Int = 0,
    val partitionHealEvents: Int = 0,
    val batteryImpact: String = "LOW",
    val networkHealthScore: Int = 100, // 0 to 100
    val nodeConfidenceMap: Map<String, NodeConfidence> = emptyMap(),
    val routeConfidenceList: List<RouteConfidence> = emptyList()
)

@Singleton
class MeshHealthManager @Inject constructor() {

    private val nodeConfidenceMap = ConcurrentHashMap<String, NodeConfidence>()
    private val routeConfidenceList = mutableListOf<RouteConfidence>()

    private val _healthMetrics = MutableStateFlow(MeshHealthMetrics())
    val healthMetrics: StateFlow<MeshHealthMetrics> = _healthMetrics.asStateFlow()

    fun updateNodeConfidence(nodeId: String, batteryLevel: Int, lastSeenMs: Long, isReachable: Boolean) {
        val now = System.currentTimeMillis()
        val ageMs = now - lastSeenMs

        // Continuous confidence score decay over time (0.0 to 1.0)
        var score = 1.0f
        if (!isReachable) {
            score = 0.0f
        } else if (ageMs > 30_000L) {
            val ageDecay = minOf(0.8f, (ageMs - 30_000L) / 60_000.0f)
            score -= ageDecay
        }

        if (batteryLevel in 0..15) {
            score -= 0.2f
        }

        val confidence = NodeConfidence(
            nodeId = nodeId,
            confidenceScore = score.coerceIn(0.0f, 1.0f),
            lastSeenMs = lastSeenMs,
            batteryLevel = batteryLevel,
            isReachable = isReachable
        )
        nodeConfidenceMap[nodeId] = confidence
        recalculateHealthScore()
    }

    fun getNodeConfidence(nodeId: String): Float {
        return nodeConfidenceMap[nodeId]?.confidenceScore ?: 0.5f
    }

    fun updateRouteConfidences(routes: List<RouteConfidence>) {
        synchronized(routeConfidenceList) {
            routeConfidenceList.clear()
            routeConfidenceList.addAll(routes)
        }
        recalculateHealthScore()
    }

    fun updateMetricsSnapshot(
        meshSize: Int,
        connectedPeersCount: Int,
        congestionLevel: CongestionLevel,
        transports: Map<RouteType, TransportHealthMetrics>,
        avgRtt: Long,
        lossRate: Float,
        retries: Long,
        repairs: Int,
        discoveries: Int,
        queueSize: Int,
        partitionSplits: Int,
        partitionHeals: Int,
        batteryImpact: String
    ) {
        val healthyRoutes = routeConfidenceList.count { it.confidenceScore >= 0.5f }
        val failedRoutes = routeConfidenceList.count { it.confidenceScore < 0.5f }

        _healthMetrics.update { current ->
            current.copy(
                meshSize = meshSize,
                connectedPeersCount = connectedPeersCount,
                healthyRoutesCount = healthyRoutes,
                failedRoutesCount = failedRoutes,
                congestionLevel = congestionLevel,
                transportHealthMap = transports,
                averageRttMs = avgRtt,
                packetLossRate = lossRate,
                totalRetries = retries,
                totalRepairs = repairs,
                discoveryCount = discoveries,
                pendingQueueSize = queueSize,
                partitionEvents = partitionSplits,
                partitionHealEvents = partitionHeals,
                batteryImpact = batteryImpact,
                nodeConfidenceMap = nodeConfidenceMap.toMap(),
                routeConfidenceList = ArrayList(routeConfidenceList)
            )
        }
        recalculateHealthScore()
    }

    private fun recalculateHealthScore() {
        var score = 100

        val metrics = _healthMetrics.value

        // Deduct for high congestion
        when (metrics.congestionLevel) {
            CongestionLevel.MEDIUM -> score -= 10
            CongestionLevel.HIGH -> score -= 30
            CongestionLevel.CRITICAL -> score -= 50
            else -> {}
        }

        // Deduct for high packet loss
        if (metrics.packetLossRate > 0.1f) {
            score -= (metrics.packetLossRate * 100).toInt().coerceAtMost(30)
        }

        // Deduct if transport confidence is low
        val bleConf = metrics.transportHealthMap[RouteType.BLE]?.confidenceScore ?: 1.0f
        if (bleConf < 0.5f) score -= 15

        // Deduct if failed routes outnumber healthy routes
        if (metrics.healthyRoutesCount + metrics.failedRoutesCount > 0) {
            val failureRatio = metrics.failedRoutesCount.toFloat() / (metrics.healthyRoutesCount + metrics.failedRoutesCount)
            if (failureRatio > 0.3f) {
                score -= (failureRatio * 30).toInt()
            }
        }

        val finalScore = score.coerceIn(0, 100)
        if (_healthMetrics.value.networkHealthScore != finalScore) {
            _healthMetrics.update { it.copy(networkHealthScore = finalScore) }
        }
    }
}
