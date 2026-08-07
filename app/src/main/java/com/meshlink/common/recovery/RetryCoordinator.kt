package com.meshlink.common.recovery

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.data.mapper.toDomain
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.RelayDao
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.messaging.data.MessageStateMachine
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.IntelligentRetryEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class RetryCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshRepositoryProvider: javax.inject.Provider<MeshRepository>,
    private val meshRouter: Router,
    private val chatDao: ChatDao,
    private val relayDao: RelayDao,
    private val intelligentRetryEngine: IntelligentRetryEngine,
    private val stateMachine: MessageStateMachine,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "RetryCoordinator"
        const val MESSAGE_TTL_MS = 86_400_000L // 24 Hours TTL
        private const val PERIODIC_TIMER_INTERVAL_MS = 300_000L // 5 minutes periodic fallback
    }

    private var isRunning = false
    private val retryAttemptMap = ConcurrentHashMap<String, Int>()
    private val scheduledJobs = ConcurrentHashMap<String, Job>()
    private var periodicJob: Job? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        MeshLogger.d(TAG, "Starting centralized RetryCoordinator")
        
        recoverPendingQueues()
        startPeriodicRetryLoop()
    }

    fun stop() {
        isRunning = false
        periodicJob?.cancel()
        scheduledJobs.values.forEach { it.cancel() }
        scheduledJobs.clear()
        retryAttemptMap.clear()
        MeshLogger.d(TAG, "Stopped RetryCoordinator")
    }

    /**
     * Called when a network event occurs (peer connected, relay discovered, route updated, BLE restart, etc.)
     * Triggers immediate evaluation and dispatch of all pending retries.
     */
    fun triggerEvent(reason: String) {
        if (!isRunning) return
        MeshLogger.d(TAG, "Event triggered retry loop. Reason: $reason")
        applicationScope.launch {
            processPendingRetries(isEventTriggered = true)
        }
    }

    fun cancelRetryForPacket(messageId: String) {
        retryAttemptMap.remove(messageId)
        scheduledJobs.remove(messageId)?.cancel()
    }

    fun triggerImmediateRetryForPacket(messageId: String) {
        applicationScope.launch {
            val message = chatDao.getMessageByUuid(messageId) ?: return@launch
            if (isPendingDeliveryStatus(message.status)) {
                retrySingleMessage(message, isImmediate = true)
            }
        }
    }

    private fun recoverPendingQueues() {
        applicationScope.launch {
            try {
                processPendingRetries(isEventTriggered = true)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to recover pending queues", e)
            }
        }
    }

    private fun startPeriodicRetryLoop() {
        periodicJob?.cancel()
        periodicJob = applicationScope.launch {
            while (isActive && isRunning) {
                delay(PERIODIC_TIMER_INTERVAL_MS)
                if (intelligentRetryEngine.shouldRetryNow()) {
                    MeshLogger.d(TAG, "Periodic retry timer fired.")
                    processPendingRetries(isEventTriggered = false)
                }
            }
        }
    }

    private suspend fun processPendingRetries(isEventTriggered: Boolean) {
        val nonTerminalStatuses = listOf(
            DeliveryStatus.QUEUED,
            DeliveryStatus.RETRYING,
            DeliveryStatus.WAITING_FOR_ROUTE,
            DeliveryStatus.WAITING_FOR_ACK,
            DeliveryStatus.SENDING,
            DeliveryStatus.PENDING
        )

        val pendingMessages = nonTerminalStatuses.flatMap { status ->
            chatDao.getMessagesByStatus(status)
        }.distinctBy { it.messageId }

        if (pendingMessages.isEmpty()) return

        MeshLogger.d(TAG, "Processing ${pendingMessages.size} pending messages for retry (Event: $isEventTriggered)...")

        val now = System.currentTimeMillis()
        pendingMessages.forEach { msg ->
            // Check TTL expiration
            if (now - msg.timestamp > MESSAGE_TTL_MS) {
                MeshLogger.w(TAG, "Message ${msg.messageId} TTL expired (${now - msg.timestamp} ms). Transitioning to EXPIRED.")
                cancelRetryForPacket(msg.messageId)
                stateMachine.transitionToExpired(msg.messageId)
                return@forEach
            }

            retrySingleMessage(msg, isImmediate = isEventTriggered)
        }
    }

    private fun retrySingleMessage(msg: com.meshlink.database.data.local.MessageEntity, isImmediate: Boolean) {
        val messageId = msg.messageId
        val attempt = (retryAttemptMap[messageId] ?: 0) + 1
        retryAttemptMap[messageId] = attempt

        val delayMs = if (isImmediate || attempt <= 1) 0L else intelligentRetryEngine.calculateRetryDelay(attempt)

        scheduledJobs[messageId]?.cancel()
        scheduledJobs[messageId] = applicationScope.launch {
            if (delayMs > 0) {
                stateMachine.transitionToRetrying(messageId)
                delay(delayMs)
            }

            // Re-verify DB status after delay/waking to ensure ACK did not arrive during delay
            val latestMsg = chatDao.getMessageByUuid(messageId)
            if (latestMsg == null || !isPendingDeliveryStatus(latestMsg.status)) {
                MeshLogger.d(TAG, "Aborting retry for $messageId: current status is ${latestMsg?.status}")
                cancelRetryForPacket(messageId)
                return@launch
            }

            if (!intelligentRetryEngine.shouldRetryNow()) {
                MeshLogger.d(TAG, "Skipping retry attempt $attempt for $messageId due to battery/congestion")
                return@launch
            }

            try {
                MeshLogger.d(TAG, "Attempting delivery attempt $attempt for message $messageId to ${msg.chatId}")
                val transitioned = stateMachine.transitionToSending(messageId)
                if (!transitioned) {
                    MeshLogger.w(TAG, "Aborting retry for $messageId: failed to transition to SENDING")
                    cancelRetryForPacket(messageId)
                    return@launch
                }
                
                val domainMsg = latestMsg.toDomain()
                meshRepositoryProvider.get().sendMessage(msg.chatId, domainMsg)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Retry attempt $attempt failed for message $messageId: ${e.message}")
                stateMachine.transitionToWaitingForRoute(messageId)
            } finally {
                scheduledJobs.remove(messageId)
            }
        }
    }

    private fun isPendingDeliveryStatus(status: DeliveryStatus): Boolean {
        return when (status) {
            DeliveryStatus.QUEUED,
            DeliveryStatus.RETRYING,
            DeliveryStatus.WAITING_FOR_ROUTE,
            DeliveryStatus.WAITING_FOR_ACK,
            DeliveryStatus.SENDING,
            DeliveryStatus.PENDING -> true
            else -> false
        }
    }
}
