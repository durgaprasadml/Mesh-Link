package com.meshlink.emergency

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyManager @Inject constructor() {
    
    companion object {
        private const val TAG = "EmergencyManager"
    }

    private val _isEmergencyModeActive = MutableStateFlow(false)
    val isEmergencyModeActive: StateFlow<Boolean> = _isEmergencyModeActive.asStateFlow()
    
    /**
     * Toggles the global emergency state.
     * When active, all battery constraints are ignored and background traffic is paused
     * to dedicate 100% of network capacity to CRITICAL emergency operations.
     */
    fun setEmergencyMode(active: Boolean) {
        if (_isEmergencyModeActive.value != active) {
            _isEmergencyModeActive.value = active
            if (active) {
                MeshLogger.w(TAG, "🚨 EMERGENCY MODE ACTIVATED 🚨 - Preempting normal traffic, ignoring battery constraints.")
            } else {
                MeshLogger.d(TAG, "Emergency Mode deactivated. Restoring standard mesh rules.")
            }
        }
    }
}
