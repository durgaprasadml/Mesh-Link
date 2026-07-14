package com.meshlink.util

import com.meshlink.domain.model.BleDevice

object MockPeerFactory {
    fun createPeer(
        meshId: String = "mesh_0",
        address: String = "00:11:22:33:44:55",
        name: String = "TestPeer",
        rssi: Int = -50
    ): BleDevice {
        return BleDevice(
            meshId = meshId,
            address = address,
            name = name,
            rssi = rssi,
            lastSeen = System.currentTimeMillis()
        )
    }
}
