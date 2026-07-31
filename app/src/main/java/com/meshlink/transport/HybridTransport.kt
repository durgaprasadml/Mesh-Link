package com.meshlink.transport

import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.transport.Transport
import kotlinx.coroutines.flow.StateFlow

enum class HybridMode {
    BLE_ONLY,
    WIFI_DIRECT_ONLY,
    HYBRID_ACTIVE
}

/**
 * Interface representing the unified, transport-agnostic Hybrid Transport layer (BLE + Wi-Fi Direct).
 */
interface HybridTransport : Transport {
    val activeMode: StateFlow<HybridMode>
    val metrics: StateFlow<HybridTransportMetrics>
    val isWifiConnected: Boolean
    val isBleConnected: Boolean

    fun getSelectedRouteType(
        targetId: String,
        packetType: PacketType,
        payloadSize: Long,
        batteryLevel: Int = 100,
        rssi: Int = -65,
        queueSize: Int = 0
    ): RouteType

    fun triggerAutoUpgrade(peerAddress: String)
    fun recordPacketMetrics(routeType: RouteType, latencyMs: Long, success: Boolean, packetSize: Long)
}
