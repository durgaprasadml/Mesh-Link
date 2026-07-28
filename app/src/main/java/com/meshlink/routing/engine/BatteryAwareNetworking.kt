package com.meshlink.routing.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PowerState {
    NORMAL,
    POWER_SAVER,
    CRITICAL
}

@Singleton
class BatteryAwareNetworking @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BatteryAwareNetworking"
        private const val CRITICAL_BATTERY_THRESHOLD = 15
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val _powerState = MutableStateFlow(PowerState.NORMAL)
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private val _batteryPct = MutableStateFlow(100)
    val batteryPct: StateFlow<Int> = _batteryPct.asStateFlow()

    init {
        updatePowerState()
    }

    fun updatePowerState() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val currentBatteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100
        
        _batteryPct.value = currentBatteryPct

        val isPowerSaveMode = powerManager.isPowerSaveMode

        val newState = when {
            currentBatteryPct <= CRITICAL_BATTERY_THRESHOLD -> PowerState.CRITICAL
            isPowerSaveMode -> PowerState.POWER_SAVER
            else -> PowerState.NORMAL
        }
        
        if (_powerState.value != newState) {
            _powerState.value = newState
            MeshLogger.d(TAG, "Power state transitioned to $newState (Battery: $currentBatteryPct%)")
        }
    }

    /**
     * Determines if the device should participate in generic mesh relays.
     * If critical, we ONLY relay SOS/Critical packets, UNLESS emergency mode is forced globally.
     */
    fun canRelayBackgroundTraffic(): Boolean {

        return _powerState.value != PowerState.CRITICAL
    }
    
    /**
     * Reduces broadcast frequency if power saver is enabled.
     */
    fun getBroadcastProbability(): Float {

        
        return when (_powerState.value) {
            PowerState.NORMAL -> 1.0f
            PowerState.POWER_SAVER -> 0.5f // 50% chance to relay broadcast
            PowerState.CRITICAL -> 0.1f // 10% chance
        }
    }
}
