package com.meshlink.recovery.engine

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SystemDegradationLevel {
    NORMAL, WARNING, CRITICAL
}

@Singleton
class BusinessContinuityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun assessSystemHealth(): SystemDegradationLevel {
        val storageLevel = getAvailableStoragePercent()
        
        if (storageLevel < 5) {
            // Critical: Less than 5% storage remaining.
            // Action: Disable all large file transfers, stop background scanning, 
            // only allow emergency text messages to preserve DB space.
            return SystemDegradationLevel.CRITICAL
        }
        
        if (storageLevel < 15) {
            return SystemDegradationLevel.WARNING
        }
        
        return SystemDegradationLevel.NORMAL
    }

    private fun getAvailableStoragePercent(): Int {
        return try {
            val stat = android.os.StatFs(Environment.getDataDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            if (totalBytes > 0) ((availableBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 100
        } catch (e: Exception) {
            100
        }
    }
}
