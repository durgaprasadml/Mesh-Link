package com.meshlink.analytics.engine

import javax.inject.Inject
import javax.inject.Singleton

data class RoutingScore(
    val cacheHitRatio: Float = 0f,
    val duplicateSuppressionRate: Float = 0f,
    val routeRepairSuccessRate: Float = 0f,
    val deadEndRoutesCount: Int = 0
)

@Singleton
class RoutingAnalytics @Inject constructor() {

    private var cacheHits = 0
    private var cacheMisses = 0
    private var duplicatesSuppressed = 0
    private var totalBroadcasts = 0

    fun recordCacheHit() { cacheHits++ }
    fun recordCacheMiss() { cacheMisses++ }
    fun recordDuplicatePacket() { duplicatesSuppressed++ }
    fun recordBroadcast() { totalBroadcasts++ }

    fun getRoutingScore(): RoutingScore {
        val hitRatio = if (cacheHits + cacheMisses == 0) 0f else cacheHits.toFloat() / (cacheHits + cacheMisses)
        val suppressionRate = if (totalBroadcasts == 0) 0f else duplicatesSuppressed.toFloat() / totalBroadcasts
        
        return RoutingScore(
            cacheHitRatio = hitRatio,
            duplicateSuppressionRate = suppressionRate
        )
    }
}
