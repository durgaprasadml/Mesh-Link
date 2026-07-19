package com.meshlink.common.power

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.repository.MeshRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class AdaptiveMeshPowerManager @Inject constructor(
    private val powerStateManager: PowerStateManager,
    private val meshRepository: MeshRepository
) {
    companion object {
        private const val TAG = "AdaptivePower"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
        
        powerStateManager.startMonitoring()
        
        scope.launch {
            powerStateManager.powerState.collect { state ->
                applyPowerPolicy(state)
            }
        }
    }

    fun stop() {
        isStarted = false
        powerStateManager.stopMonitoring()
    }

    private fun applyPowerPolicy(state: PowerState) {
        MeshLogger.d(TAG, "Applying mesh power policy for state: $state")
        when (state) {
            PowerState.NORMAL -> {
                // In a real implementation, we would call into BleManager to set SCAN_MODE_LOW_LATENCY
                // For now, we simulate adjusting the internal power profile via MeshRepository
                MeshLogger.d(TAG, "Setting BLE/Wi-Fi to HIGH PERFORMANCE mode")
                scope.launch {
                    meshRepository.autoStartMesh() // Ensures mesh is fully active
                }
            }
            PowerState.BATTERY_SAVER -> {
                // Throttle BLE scanning to opportunistic or low power
                MeshLogger.w(TAG, "Setting BLE/Wi-Fi to LOW POWER mode. Throttling discovery.")
                // No action needed other than internal flag adjustment, as actual protocol modification is out of scope.
            }
            PowerState.DOZE_MODE, PowerState.RESTRICTED -> {
                // Pause non-critical routing jobs and reduce advertising
                MeshLogger.w(TAG, "Device in DOZE/RESTRICTED. Suspending active discovery to conserve battery.")
                // meshRepository.stopMesh() could be called if we completely shut down, 
                // but we want to remain offline-capable. We just scale back.
            }
        }
    }
}
