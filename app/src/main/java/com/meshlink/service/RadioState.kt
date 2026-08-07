package com.meshlink.service

enum class RadioSubsystem {
    BLE_SCANNER,
    BLE_ADVERTISER,
    GATT_SERVER,
    WIFI_DIRECT,
    DISCOVERY_ENGINE,
    ROUTING_ENGINE,
    PACKET_DISPATCHER
}

enum class RadioState {
    INITIALIZING,
    RUNNING,
    RECOVERING,
    STOPPED,
    FAILED
}
