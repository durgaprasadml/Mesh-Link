package com.meshlink.service

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.oem.OemCompatibilityManager
import com.meshlink.common.power.BatteryOptimizationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class MeshLifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshSupervisor: MeshSupervisor,
    private val meshWatchdog: MeshWatchdog,
    private val oemCompatibilityManager: OemCompatibilityManager,
    private val batteryOptimizationManager: BatteryOptimizationManager,
    private val meshSessionManager: MeshSessionManager
) {
    companion object {
        private const val TAG = "MeshLifecycleManager"
    }

    private val _isMeshRunning = MutableStateFlow(false)
    val isMeshRunning: StateFlow<Boolean> = _isMeshRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    fun initialize() {
        MeshLogger.d(TAG, "Initializing Mesh Lifecycle Manager")
        oemCompatibilityManager.applyVendorOptimizations()
        meshSessionManager.restoreSession()
    }

    fun startMesh() {
        if (_isMeshRunning.value) {
            MeshLogger.d(TAG, "Mesh engine is already running. Refreshing.")
            meshSupervisor.startAllSubsystems()
            return
        }

        MeshLogger.d(TAG, "Starting Mesh Engine Lifecycle")
        _isMeshRunning.value = true
        _isPaused.value = false

        meshSupervisor.startAllSubsystems()
        meshWatchdog.start()
    }

    fun stopMesh() {
        MeshLogger.d(TAG, "Stopping Mesh Engine Lifecycle")
        _isMeshRunning.value = false
        _isPaused.value = false

        meshWatchdog.stop()
        meshSupervisor.stopAllSubsystems()
    }

    fun restartMesh() {
        MeshLogger.d(TAG, "Restarting Mesh Engine Lifecycle")
        stopMesh()
        startMesh()
    }

    fun pauseMesh() {
        if (!_isMeshRunning.value || _isPaused.value) return
        MeshLogger.d(TAG, "Pausing non-essential mesh radio scanning due to power policy")
        _isPaused.value = true
    }

    fun resumeMesh() {
        if (!_isMeshRunning.value || !_isPaused.value) return
        MeshLogger.d(TAG, "Resuming mesh radio active duty cycles")
        _isPaused.value = false
        meshSupervisor.startAllSubsystems()
    }

    fun initializeAfterOnboarding() {
        MeshLogger.d(TAG, "[MeshStartup] ONBOARDING_COMPLETED: Explicitly initializing mesh engine post-onboarding")
        initialize()
        _isMeshRunning.value = true
        _isPaused.value = false
        meshSupervisor.forceRestartAllSubsystems()
        meshWatchdog.start()
    }

    fun forceInitialize() {
        MeshLogger.d(TAG, "[MeshStartup] Force initializing mesh engine")
        _isMeshRunning.value = true
        _isPaused.value = false
        meshSupervisor.forceRestartAllSubsystems()
        meshWatchdog.start()
    }

    fun isFullyOperational(): Boolean {
        return _isMeshRunning.value && meshSupervisor.isFullyOperational()
    }

    fun isMeshRunning(): Boolean = _isMeshRunning.value
}
