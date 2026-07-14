package com.meshlink.util

class FakeClock {
    var currentTimeMillis: Long = 0L
    
    fun advanceBy(millis: Long) {
        currentTimeMillis += millis
    }
    
    fun advanceTo(timeMillis: Long) {
        currentTimeMillis = timeMillis
    }
    
    fun now(): Long = currentTimeMillis
}
