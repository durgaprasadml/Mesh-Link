package com.meshlink.ble.data.source

import com.meshlink.domain.model.BleDevice
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleMeshDataSource {
    val scannedDevices: StateFlow<Map<String, BleDevice>>
    val isBleEnabled: Boolean
    val isAdvertising: Boolean
    val isScanning: Boolean

    fun startAdvertising(name: String, meshId: String, capabilities: Byte = 0)
    fun stopAdvertising()
    fun startScanning()
    fun stopScanning()

    val activeClients: Set<String>
    val connectedServers: Set<String>
    
    fun startServer()
    fun stopServer()


    fun connectToDevice(address: String)
    fun disconnectFromDevice(address: String)
    
    val incomingPayloads: SharedFlow<Pair<String, String>> // address, payload
}
