package com.meshlink.common.util

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
}
