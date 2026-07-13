package com.meshlink.ble.data

import java.util.UUID

object BleConstants {
    // Arbitrary unique UUIDs for our Mesh Link service
    val MESH_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    val MSG_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    private const val NETWORK_ID_LENGTH = 8
    private val bluetoothAddressRegex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")

    fun toNetworkId(meshId: String): String {
        return meshId.trim().take(NETWORK_ID_LENGTH)
    }

    fun isBluetoothAddress(value: String): Boolean {
        return bluetoothAddressRegex.matches(value.trim())
    }
}
