package com.meshlink.common.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    BATTERY_SAVER,
    DOZE_MODE,
    RESTRICTED
}

@Singleton
class PowerStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PowerStateManager"
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val _powerState = MutableStateFlow(getCurrentPowerState())
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newState = getCurrentPowerState()
            if (_powerState.value != newState) {
                MeshLogger.w(TAG, "Power state changed: ${_powerState.value} -> $newState")
                _powerState.value = newState
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        _powerState.value = getCurrentPowerState()
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error unregistering power receiver", e)
        }
    }

    private fun getCurrentPowerState(): PowerState {
        if (powerManager.isDeviceIdleMode) {
            return PowerState.DOZE_MODE
        }
        if (powerManager.isPowerSaveMode) {
            return PowerState.BATTERY_SAVER
        }
        return PowerState.NORMAL
    }
}
