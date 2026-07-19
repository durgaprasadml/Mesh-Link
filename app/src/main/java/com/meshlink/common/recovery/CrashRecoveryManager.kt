package com.meshlink.common.recovery

import android.content.Context
import android.content.Intent
import android.os.Build
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.service.MeshRelayService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Singleton
class CrashRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateRestorationManager: StateRestorationManager,
    private val meshRepository: MeshRepository
) {
    companion object {
        private const val TAG = "CrashRecovery"
        private const val RECOVERY_PREFS = "meshlink_crash_recovery"
        private const val KEY_LAST_KNOWN_STATE = "last_known_state"
        private const val STATE_CLEAN_EXIT = "clean_exit"
        private const val STATE_RUNNING = "running"
    }

    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(RECOVERY_PREFS, Context.MODE_PRIVATE)

    fun initialize() {
        val lastState = prefs.getString(KEY_LAST_KNOWN_STATE, STATE_CLEAN_EXIT)
        if (lastState == STATE_RUNNING) {
            MeshLogger.w(TAG, "Application recovered from an unexpected shutdown or crash.")
            performRecovery()
        } else {
            MeshLogger.d(TAG, "Application started normally.")
        }
        
        // Mark as running for the next session
        prefs.edit().putString(KEY_LAST_KNOWN_STATE, STATE_RUNNING).apply()
        
        // Setup default unhandled exception handler to attempt clean exit marker if possible
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            MeshLogger.e(TAG, "FATAL CRASH on thread ${thread.name}", exception)
            // We do NOT mark clean exit here, so next start triggers recovery
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    fun markCleanExit() {
        prefs.edit().putString(KEY_LAST_KNOWN_STATE, STATE_CLEAN_EXIT).apply()
        MeshLogger.d(TAG, "Application marked for clean exit.")
    }

    private fun performRecovery() {
        recoveryScope.launch {
            try {
                // 1. Check restoration state
                val state = stateRestorationManager.stateFlow.firstOrNull()
                MeshLogger.d(TAG, "Recovering state: ${state?.lastScreen}")

                // 2. Restart background services if they were active
                restartForegroundService()

                // 3. Kickstart mesh networking
                meshRepository.autoStartMesh()
                
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Error during crash recovery", e)
            }
        }
    }

    private fun restartForegroundService() {
        try {
            val intent = Intent(context, MeshRelayService::class.java).apply {
                action = MeshRelayService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            MeshLogger.d(TAG, "MeshRelayService recovery triggered.")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to recover MeshRelayService", e)
        }
    }
}
