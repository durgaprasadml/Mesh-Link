package com.meshlink.wifi.model

import android.net.wifi.p2p.WifiP2pDevice
import com.meshlink.domain.model.RouteType

data class WifiP2pDeviceModel(
    val deviceName: String = "",
    val deviceAddress: String = "",
    val status: Int = WifiP2pDevice.UNAVAILABLE,
    val isGroupOwner: Boolean = false,
    val groupOwnerAddress: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val signal: Int = -60,
    val transport: RouteType = RouteType.WIFI_DIRECT,
    val connectionTimestamp: Long? = null
) {
    val statusString: String
        get() = when (status) {
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
}
