package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.meshlink.ble.data.BleGattManager.BleConnectionState

/**
 * Manages GATT connections (both client and server) and their states.
 *
 * Responsibility: Tracks active clients, connected servers, and connection states.
 * Thread Ownership: Methods should be called in a thread-safe manner (e.g., using Coroutines/Mutex).
 * Lifecycle Ownership: Application scoped.
 * Dependencies: None.
 */
interface GattConnectionManager {
    val connectedServers: Map<String, BluetoothDevice>
    val activeClients: Map<String, BluetoothGatt>
    val deviceStates: Map<String, BleConnectionState>

    fun addConnectedServer(address: String, device: BluetoothDevice)
    fun removeConnectedServer(address: String)
    fun addActiveClient(address: String, gatt: BluetoothGatt)
    fun removeActiveClient(address: String)
    fun updateDeviceState(address: String, state: BleConnectionState)
    fun getDeviceState(address: String): BleConnectionState?
    fun getClient(address: String): BluetoothGatt?
    fun clear()
}
