package com.meshlink.messaging.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.routing.api.Router
import com.meshlink.routing.api.PacketStatusEvent
import com.meshlink.routing.api.PacketQueued
import com.meshlink.routing.api.PacketTransmissionStarted
import com.meshlink.routing.api.PacketTransmitted
import com.meshlink.routing.api.PacketFailed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryTracker @Inject constructor(
    private val router: Router,
    private val stateMachine: MessageStateMachine,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "DeliveryTracker"
        const val DELIVERY_TIMEOUT_MS = 30_000L
    }

    private val timeoutJobs = ConcurrentHashMap<String, Job>()
    
    init {
        applicationScope.launch {
            router.packetEvents.collect { event ->
                handlePacketEvent(event)
            }
        }
    }

    private suspend fun handlePacketEvent(event: PacketStatusEvent) {
        when (event) {
            is PacketQueued -> {
                stateMachine.transitionToQueued(event.packetId)
            }
            is PacketTransmissionStarted -> {
                stateMachine.transitionToSending(event.packetId)
            }
            is PacketTransmitted -> {
                stateMachine.transitionToWaitingForAck(event.packetId)
                startDeliveryTimeout(event.packetId)
            }
            is PacketFailed -> {
                cancelTimeout(event.packetId)
                stateMachine.transitionToWaitingForRoute(event.packetId)
                MeshLogger.w(TAG, "Packet ${event.packetId} transmission failed: ${event.cause?.message}. Transitioned to WAITING_FOR_ROUTE")
            }
        }
    }

    private fun startDeliveryTimeout(packetId: String) {
        cancelTimeout(packetId)
        timeoutJobs[packetId] = applicationScope.launch {
            delay(DELIVERY_TIMEOUT_MS)
            MeshLogger.w(TAG, "Delivery ACK timeout for packet $packetId - transitioning to RETRYING")
            stateMachine.transitionToRetrying(packetId)
            timeoutJobs.remove(packetId)
        }
    }

    private fun cancelTimeout(packetId: String) {
        timeoutJobs.remove(packetId)?.cancel()
    }

    suspend fun onAckReceived(packetId: String) {
        cancelTimeout(packetId)
        stateMachine.transitionToDelivered(packetId)
        MeshLogger.d(TAG, "ACK received for $packetId, transitioned to DELIVERED")
    }

    suspend fun onReadReceiptReceived(packetId: String) {
        cancelTimeout(packetId)
        stateMachine.transitionToSeen(packetId)
    }
}
