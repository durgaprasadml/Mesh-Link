package com.meshlink.common.config

/**
 * Centralized configuration for Bluetooth Low Energy operations.
 *
 * Responsibility: Holds constants and default values for BLE tuning, MTU settings,
 * queue limits, and fragmentation parameters.
 * Thread Ownership: Thread-safe (compile-time constants).
 * Lifecycle Ownership: Application scoped (Singleton object).
 * Dependencies: None.
 */
object BleConfig {
    const val DEFAULT_MTU = 23
    const val MAX_ATTRIBUTE_VALUE_SIZE = 512
    const val GATT_HEADER_SIZE = 3
    const val FRAG_HEADER_SIZE = 1
    const val GATT_RETRY_LIMIT = 10
    const val MAX_MTU_REQUEST = 512
}
