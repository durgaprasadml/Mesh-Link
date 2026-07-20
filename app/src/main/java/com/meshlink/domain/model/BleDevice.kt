package com.meshlink.domain.model

enum class TransportType {
    BLE, WIFI_DIRECT, HYBRID
}

data class BleDevice(
    val meshId: String,
    val name: String,
    val address: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val transport: TransportType = TransportType.BLE,
    val capabilities: Byte = 0,
    val isConnected: Boolean = false
)
