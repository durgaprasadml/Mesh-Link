package com.meshlink.scalability.engine

import javax.inject.Inject
import javax.inject.Singleton

enum class ScalabilityScore {
    EXCELLENT, GOOD, WARNING, CRITICAL
}

data class ScalabilityMetrics(
    val meshDensity: Int, // average edges per node
    val networkDiameter: Int,
    val connectedClusters: Int,
    val totalNodes: Int
)

@Singleton
class MeshScalabilityManager @Inject constructor() {

    fun computeScalabilityScore(metrics: ScalabilityMetrics): ScalabilityScore {
        if (metrics.connectedClusters > 5) {
            // Highly fractured network
            return ScalabilityScore.CRITICAL
        }
        
        if (metrics.meshDensity > 15) {
            // Ultra-dense, risk of broadcast storms
            return ScalabilityScore.WARNING
        }
        
        if (metrics.networkDiameter > 10) {
            // Very stretched network, latency will be high
            return ScalabilityScore.WARNING
        }
        
        if (metrics.totalNodes > 200) {
            return ScalabilityScore.GOOD
        }
        
        return ScalabilityScore.EXCELLENT
    }
}
