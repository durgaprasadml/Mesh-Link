package com.meshlink.ble.data

import android.content.Context
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.domain.model.BleDevice
import com.meshlink.ble.data.source.BleMeshDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleDataSource: BleMeshDataSource,
    val discoveryEngine: DiscoveryEngine
) {

    val scannedDevices: StateFlow<Map<String, BleDevice>> = bleDataSource.scannedDevices

    fun startAdvertising(localUserName: String, localMeshId: String, batteryLevel: Int = 100) {
        bleDataSource.startAdvertising(localUserName, localMeshId, batteryLevel.toByte())
    }

    fun stopAdvertising() {
        bleDataSource.stopAdvertising()
    }

    fun startScanning() {
        bleDataSource.startScanning()
    }

    fun stopScanning() {
        bleDataSource.stopScanning()
    }

    fun isAdvertising(): Boolean = bleDataSource.isAdvertising
    
    fun isScanning(): Boolean = bleDataSource.isScanning
}
