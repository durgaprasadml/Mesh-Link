package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

/**
 * Manages sending notifications from the GATT Server to connected clients.
 *
 * Responsibility: Sending characteristic notifications to remote devices.
 * Thread Ownership: Safe to call concurrently (GATT framework handles synchronization).
 * Lifecycle Ownership: Application scoped.
 * Dependencies: BluetoothGattServer.
 */
interface GattNotificationManager {
    fun notifyCharacteristic(device: BluetoothDevice, char: BluetoothGattCharacteristic, value: ByteArray)
    fun onNotificationSent(device: BluetoothDevice, status: Int)
    fun clear(address: String)
}
