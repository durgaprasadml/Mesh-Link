package com.meshlink.domain.model

enum class TransportType {
    BLE
}

data class BleDevice(
    val meshId: String,
    val name: String,
    val address: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val transport: TransportType = TransportType.BLE,
    val capabilities: Byte = 0,
    val isConnected: Boolean = false,
    val distanceMeters: Double? = null,
    val distanceConfidence: String? = null,
    val avatarUri: String? = null,
    val hopCount: Int = 0,
    val isMeshNode: Boolean = false,
    val viaRelayId: String? = null
)
