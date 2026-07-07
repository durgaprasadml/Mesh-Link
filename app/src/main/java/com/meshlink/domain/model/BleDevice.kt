package com.meshlink.domain.model

data class BleDevice(
    val meshId: String,
    val name: String,
    val address: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis()
)
