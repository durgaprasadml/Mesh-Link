package com.meshlink.ble.discovery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PowerState {
    OPTIMAL,
    BALANCED,
    RESTRICTED
}

/**
 * Monitors battery and thermal states to advise the DiscoveryEngine on power usage.
 */
@Singleton
class BatteryAwareScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * Determines the current power state.
     */
    fun getCurrentPowerState(): PowerState {
        // If power saver is on, immediately return restricted
        if (powerManager.isPowerSaveMode) {
            return PowerState.RESTRICTED
        }

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                  status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            return PowerState.OPTIMAL
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()

        return when {
            batteryPct > 50 -> PowerState.BALANCED
            batteryPct > 15 -> PowerState.BALANCED // Could be tuned
            else -> PowerState.RESTRICTED
        }
    }
}
