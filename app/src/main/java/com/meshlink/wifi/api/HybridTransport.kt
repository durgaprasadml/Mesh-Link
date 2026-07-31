package com.meshlink.wifi.api

import com.meshlink.domain.model.RouteType
import com.meshlink.domain.transport.Transport
import kotlinx.coroutines.flow.StateFlow

enum class HybridMode {
    BLE_ONLY,
    WIFI_DIRECT_ONLY,
    HYBRID_ACTIVE
}

/**
 * Interface representing the unified Hybrid Transport layer (BLE + Wi-Fi Direct).
 */
interface HybridTransport : Transport {
    val activeMode: StateFlow<HybridMode>
    val isWifiConnected: Boolean
    val isBleConnected: Boolean
    fun getSelectedRouteType(targetId: String, packetType: com.meshlink.domain.model.PacketType, payloadSize: Long): RouteType
    fun triggerAutoUpgrade(peerAddress: String)
}
