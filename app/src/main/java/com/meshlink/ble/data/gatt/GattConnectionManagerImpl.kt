package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.meshlink.ble.data.BleGattManager.BleConnectionState
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattConnectionManagerImpl @Inject constructor() : GattConnectionManager {
    private val _connectedServers = ConcurrentHashMap<String, BluetoothDevice>()
    private val _activeClients = ConcurrentHashMap<String, BluetoothGatt>()
    private val _deviceStates = ConcurrentHashMap<String, BleConnectionState>()

    override val connectedServers: Map<String, BluetoothDevice> get() = _connectedServers
    override val activeClients: Map<String, BluetoothGatt> get() = _activeClients
    override val deviceStates: Map<String, BleConnectionState> get() = _deviceStates

    override fun addConnectedServer(address: String, device: BluetoothDevice) {
        _connectedServers[address] = device
    }

    override fun removeConnectedServer(address: String) {
        _connectedServers.remove(address)
    }

    override fun addActiveClient(address: String, gatt: BluetoothGatt) {
        _activeClients[address] = gatt
    }

    override fun removeActiveClient(address: String) {
        _activeClients.remove(address)
        _deviceStates.remove(address)
    }

    override fun updateDeviceState(address: String, state: BleConnectionState) {
        _deviceStates[address] = state
    }

    override fun getDeviceState(address: String): BleConnectionState? {
        return _deviceStates[address]
    }

    override fun getClient(address: String): BluetoothGatt? {
        return _activeClients[address]
    }

    override fun clear() {
        _connectedServers.clear()
        _activeClients.clear()
        _deviceStates.clear()
    }
}
