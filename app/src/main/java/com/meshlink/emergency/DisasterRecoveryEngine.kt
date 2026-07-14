package com.meshlink.emergency

import com.meshlink.common.logger.MeshLogger
import com.meshlink.routing.engine.RoutingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisasterRecoveryEngine @Inject constructor(
    private val emergencyManager: EmergencyManager,
    private val routingEngine: RoutingEngine
) {
    companion object {
        private const val TAG = "DisasterRecoveryEngine"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRecovering = false

    /**
     * Called when the network experiences a sudden massive partition or when rebooting 
     * in the middle of an active emergency.
     */
    fun triggerMassReconnect() {
        if (isRecovering) return
        isRecovering = true
        
        scope.launch {
            MeshLogger.w(TAG, "Triggering Mass Reconnect Sequence...")
            
            // 1. Enter emergency high-power mode temporarily to blast discovery packets
            val wasEmergency = emergencyManager.isEmergencyModeActive.value
            if (!wasEmergency) {
                emergencyManager.setEmergencyMode(true)
            }
            
            // 2. Clear stale queues that might be causing a jam
            routingEngine.routeManager.routeCache.clearAllRoutes()
            
            // 3. Keep high power discovery running for 2 minutes to reform mesh
            delay(120_000L)
            
            // 4. Restore state
            if (!wasEmergency) {
                emergencyManager.setEmergencyMode(false)
            }
            
            isRecovering = false
            MeshLogger.d(TAG, "Mass Reconnect Sequence Completed.")
        }
    }
}
