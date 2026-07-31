package com.meshlink.wifi.api

import com.meshlink.domain.transport.Transport

/**
 * Interface representing the Wi-Fi Direct transport layer.
 */
interface WifiTransport : Transport {
    val isP2pEnabled: Boolean
    val isConnected: Boolean
}
