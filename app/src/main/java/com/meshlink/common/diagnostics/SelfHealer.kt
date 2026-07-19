package com.meshlink.common.diagnostics

import android.content.Context
import android.content.Intent
import com.meshlink.common.logger.MeshLogger
import com.meshlink.service.MeshRelayService
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class SelfHealer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scannerManager: BleScannerManager,
    private val advertiserManager: BleAdvertiserManager,
    private val userRepository: UserRepository
) {
    companion object {
        private const val TAG = "SelfHealer"
        private const val BACKOFF_MS = 30_000L
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val lastRecoveryTime = mutableMapOf<String, Long>()

    fun triggerRecovery(componentName: String) {
        scope.launch {
            val now = System.currentTimeMillis()
            val lastRecovery = lastRecoveryTime[componentName] ?: 0L

            if (now - lastRecovery < BACKOFF_MS) {
                MeshLogger.w(TAG, "Skipping recovery for $componentName due to backoff strategy.")
                return@launch
            }
            
            lastRecoveryTime[componentName] = now
            MeshLogger.w(TAG, "Executing Self Healing Playbook for: $componentName")

            when (componentName) {
                "MeshRelayService" -> recoverMeshService()
                "BleScanner" -> recoverBleScanner()
                "BleAdvertiser" -> recoverBleAdvertiser()
                "Database" -> recoverDatabase()
                else -> MeshLogger.e(TAG, "Unknown component for recovery: $componentName")
            }
        }
    }

    private fun recoverMeshService() {
        MeshLogger.d(TAG, "Restarting MeshRelayService...")
        try {
            val intent = Intent(context, MeshRelayService::class.java).apply {
                action = MeshRelayService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to restart MeshRelayService: ${e.message}")
        }
    }

    private fun recoverBleScanner() {
        MeshLogger.d(TAG, "Restarting BleScanner...")
        try {
            scannerManager.stopScanning()
            scannerManager.startScanning()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to restart BleScanner: ${e.message}")
        }
    }

    private suspend fun recoverBleAdvertiser() {
        MeshLogger.d(TAG, "Restarting BleAdvertiser...")
        try {
            advertiserManager.stopAdvertising()
            val user = userRepository.getLocalUser()
            if (user != null) {
                advertiserManager.startAdvertising(user.name, user.meshId, 0x01)
            } else {
                MeshLogger.w(TAG, "Cannot recover BleAdvertiser: Local user not found")
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to restart BleAdvertiser: ${e.message}")
        }
    }

    private fun recoverDatabase() {
        MeshLogger.w(TAG, "Database recovery requested. Closing connections not implemented yet.")
        // In a real scenario, we might call AppDatabase.close() and let the DI graph rebuild it.
    }
}
