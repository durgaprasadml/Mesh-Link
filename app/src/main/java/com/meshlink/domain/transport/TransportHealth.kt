package com.meshlink.domain.transport

enum class TransportHealth {
    AVAILABLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECOVERING,
    ERROR
}
