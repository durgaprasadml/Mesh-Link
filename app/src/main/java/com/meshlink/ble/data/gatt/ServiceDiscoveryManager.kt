package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothGatt

/**
 * Manages GATT service discovery and characteristic subscription.
 *
 * Responsibility: Initiates service discovery and writes CCCD descriptors to enable notifications.
 * Thread Ownership: GATT callback thread.
 * Lifecycle Ownership: Application scoped.
 * Dependencies: None.
 */
interface ServiceDiscoveryManager {
    fun discoverServices(gatt: BluetoothGatt)
    fun onServicesDiscovered(gatt: BluetoothGatt)
}
