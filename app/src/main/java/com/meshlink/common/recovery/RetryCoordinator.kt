package com.meshlink.common.recovery

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.RelayDao
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.routing.data.MeshRouter
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.BroadcastType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

@Singleton
class RetryCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshRepository: MeshRepository,
    private val meshRouter: MeshRouter,
    private val chatDao: ChatDao,
    private val relayDao: RelayDao
) {
    companion object {
        private const val TAG = "RetryCoordinator"
        private const val BASE_BACKOFF_MS = 2000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val MAX_RETRIES = 10
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        MeshLogger.d(TAG, "Starting RetryCoordinator")
        
        // 1. Recover Pending Queues (Phase 4)
        recoverPendingQueues()
        
        // 2. Start Automatic Retry Loops (Phase 8)
        startConnectionRetryLoop()
    }

    fun stop() {
        isRunning = false
        MeshLogger.d(TAG, "Stopping RetryCoordinator")
    }

    private fun recoverPendingQueues() {
        scope.launch {
            try {
                // Recover from RelayDao (Store-and-Forward)
                val storedRelays = relayDao.getAllRelayPackets()
                if (storedRelays.isNotEmpty()) {
                    MeshLogger.d(TAG, "Recovering ${storedRelays.size} stored relay packets from database.")
                    // Packets will be handled by MeshRouter's S&F loop, 
                    // but we can ensure they are evaluated without duplicates.
                }

                // Recover Pending Messages that were unsent (status = PENDING)
                val pendingMessages = chatDao.getMessagesByStatus(com.meshlink.database.data.local.DeliveryStatus.PENDING)
                if (pendingMessages.isNotEmpty()) {
                    MeshLogger.d(TAG, "Recovering ${pendingMessages.size} pending chat messages.")
                    pendingMessages.forEach { msg: com.meshlink.database.data.local.MessageEntity ->
                        // Re-queue them for sending
                        meshRouter.sendPayload(
                            targetId = msg.chatId,
                            payload = msg.text,
                            myAddressAlias = msg.senderId,
                            encrypted = true,
                            packetId = msg.messageId
                        )
                    }
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to recover pending queues", e)
            }
        }
    }

    private fun startConnectionRetryLoop() {
        scope.launch {
            var attempt = 0
            while (isActive && isRunning) {
                delay(calculateBackoff(attempt))
                
                try {
                    // Check if mesh is active, if not, attempt reconnect
                    val status = meshRepository.getMeshStatus()
                    if (!status.isBleAdvertising && !status.isBleScanning) {
                        MeshLogger.w(TAG, "Mesh is down. Attempting recovery reconnect (Attempt $attempt).")
                        meshRepository.autoStartMesh()
                        attempt++
                    } else {
                        // Reset backoff if healthy
                        attempt = 0
                        delay(10_000L) // Normal healthy check interval
                    }
                    
                    if (attempt > MAX_RETRIES) {
                        MeshLogger.e(TAG, "Max reconnect retries reached. Suspending connection retry loop to save battery.")
                        delay(MAX_BACKOFF_MS * 5) // Deep sleep before trying again
                        attempt = 0
                    }
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Error in connection retry loop", e)
                    attempt++
                }
            }
        }
    }

    private fun calculateBackoff(attempt: Int): Long {
        if (attempt == 0) return BASE_BACKOFF_MS
        val exponential = BASE_BACKOFF_MS * (2.0.pow(attempt.toDouble())).toLong()
        return minOf(exponential, MAX_BACKOFF_MS)
    }
}
