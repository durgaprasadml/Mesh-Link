package com.meshlink.util

class FakeBluetoothManager {
    var isEnabled = true
    var isAdvertising = false
    var isScanning = false
    
    fun startAdvertising() {
        isAdvertising = true
    }
    
    fun stopAdvertising() {
        isAdvertising = false
    }
    
    fun startScanning() {
        isScanning = true
    }
    
    fun stopScanning() {
        isScanning = false
    }
}
