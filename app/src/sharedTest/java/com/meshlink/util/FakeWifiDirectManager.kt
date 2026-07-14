package com.meshlink.util

class FakeWifiDirectManager {
    var isEnabled = true
    var isDiscovering = false
    
    fun startDiscovery() {
        isDiscovering = true
    }
    
    fun stopDiscovery() {
        isDiscovering = false
    }
}
