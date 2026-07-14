package com.meshlink.routing.engine

enum class RouteType {
    BLE,
    WIFI_DIRECT,
    HYBRID
}

data class RouteMetrics(
    var rssi: Int = -100, // Normalized to typical BLE range
    var packetLossRate: Float = 0f, // 0.0 to 1.0
    var averageLatencyMs: Long = 0L,
    var successfulDeliveries: Int = 0,
    var failedDeliveries: Int = 0,
    var retryCount: Int = 0,
    var pendingQueueSize: Int = 0,
    var batteryLevel: Int = -1, // -1 means unknown, 0-100%
    var trustScore: Int = 50, // Default trust, can be mapped from TrustManager
    var congestionLevel: Int = 0, // 0-100%
    var routeStability: Float = 1.0f // 0.0 to 1.0 based on recent disconnects
) {
    val historicalSuccessRate: Float
        get() {
            val total = successfulDeliveries + failedDeliveries
            return if (total == 0) 1.0f else successfulDeliveries.toFloat() / total
        }

    fun recordSuccess(latencyMs: Long) {
        successfulDeliveries++
        val total = successfulDeliveries + failedDeliveries
        packetLossRate = failedDeliveries.toFloat() / total
        
        // Exponential moving average for latency
        if (averageLatencyMs == 0L) {
            averageLatencyMs = latencyMs
        } else {
            averageLatencyMs = (0.7 * averageLatencyMs + 0.3 * latencyMs).toLong()
        }
    }
    
    fun recordFailure() {
        failedDeliveries++
        val total = successfulDeliveries + failedDeliveries
        packetLossRate = failedDeliveries.toFloat() / total
    }
}

data class RouteEntry(
    val destinationId: String,
    val nextHop: String, // The immediate peer's MAC/ID
    var hops: Int,
    val routeType: RouteType = RouteType.BLE,
    val metrics: RouteMetrics = RouteMetrics(),
    var lastSeen: Long = System.currentTimeMillis(),
    var score: Int = 0, // 0-100 calculated by RouteScorer
    var isVerified: Boolean = false // Set to true if a packet has successfully traversed this route
)
