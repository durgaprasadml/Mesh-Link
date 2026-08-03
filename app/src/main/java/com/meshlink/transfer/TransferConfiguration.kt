package com.meshlink.transfer

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized, configurable transfer policies and parameters.
 * Eliminates magic numbers across transfer pipeline components.
 */
@Singleton
class TransferConfiguration @Inject constructor() {

    var wifiWindowSize: Int = 16
    var bleWindowSize: Int = 4
    var workerCount: Int = 4

    var wifiAckTimeoutMs: Long = 2000L
    var bleAckTimeoutMs: Long = 5000L
    var retryLimit: Int = 3

    var queueCapacity: Int = 64
    var dispatchBatchSize: Int = 8

    fun getWindowSize(transportType: TransportType): Int {
        return when (transportType) {
            TransportType.WIFI_DIRECT -> wifiWindowSize
            TransportType.BLE -> bleWindowSize
            else -> bleWindowSize
        }
    }

    fun getAckTimeoutMs(transportType: TransportType): Long {
        return when (transportType) {
            TransportType.WIFI_DIRECT -> wifiAckTimeoutMs
            TransportType.BLE -> bleAckTimeoutMs
            else -> bleAckTimeoutMs
        }
    }
}
