package com.meshlink.ble.data.source

import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.ble.data.BleGattManager
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.domain.model.BleDevice
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class BleMeshDataSourceImpl @Inject constructor(
    private val scanner: BleScannerManager,
    private val advertiser: BleAdvertiserManager,
    private val gattManager: BleGattManager
) : BleMeshDataSource {

    override val scannedDevices: StateFlow<Map<String, BleDevice>> = scanner.scannedDevices
    override val isBleEnabled: Boolean get() = true
    override val isAdvertising: Boolean get() = false
    override val isScanning: Boolean get() = false

    private val _incomingPayloads = MutableSharedFlow<Pair<String, String>>()
    override val incomingPayloads: SharedFlow<Pair<String, String>> = _incomingPayloads

    override fun startAdvertising(name: String, meshId: String) {
        advertiser.startAdvertising(name, meshId)
    }

    override fun stopAdvertising() {
        advertiser.stopAdvertising()
    }

    override fun startScanning() {
        scanner.startScanning()
    }

    
    override val activeClients: Set<String> get() = gattManager.activeClients.keys
    override val connectedServers: Set<String> get() = gattManager.connectedServers.keys
    
    override fun startServer() {
        gattManager.startServer()
    }
    
    override fun stopServer() {
        gattManager.stopServer()
    }

    override fun stopScanning() {
        scanner.stopScanning()
    }

    override fun connectToDevice(address: String) {
        gattManager.connectToDevice(address)
    }

    override fun disconnectFromDevice(address: String) {
        gattManager.disconnectDevice(address)
    }
}
