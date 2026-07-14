package com.meshlink.util

class FakeLocationProvider {
    var hasLocationPermission = true
    var isLocationEnabled = true
    var currentLocation = Pair(0.0, 0.0)
    
    fun setLocation(lat: Double, lng: Double) {
        currentLocation = Pair(lat, lng)
    }
}
