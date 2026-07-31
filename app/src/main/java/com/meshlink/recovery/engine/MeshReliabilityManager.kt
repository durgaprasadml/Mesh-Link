package com.meshlink.recovery.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.routing.engine.HeartbeatManager
import com.meshlink.routing.engine.RouteCache
import com.meshlink.transport.TransportHealthMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Facade for the Network Reliability & Self-Healing Subsystem.
 * Acts as the single entry point coordinating MeshHealthManager, HeartbeatManager,
 * PartitionRecoveryManager, StoreAndForwardRecoveryManager, and TransportHealthMonitor.
 */
@Singleton
class MeshReliabilityManager @Inject constructor(
    val healthManager: MeshHealthManager,
    val heartbeatManager: HeartbeatManager,
    val partitionRecoveryManager: PartitionRecoveryManager,
    val storeAndForwardManager: StoreAndForwardRecoveryManager,
    val transportHealthMonitor: TransportHealthMonitor,
    private val congestionMonitor: CongestionMonitor,
    private val routeCache: RouteCache,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MeshReliabilityManager"
    }

    private var localMeshId: String = ""
    private var sendPacketFunc: (suspend (MeshPacket) -> Unit)? = null

    val healthMetrics: StateFlow<MeshHealthMetrics>
        get() = healthManager.healthMetrics

    /**
     * Starts the self-healing subsystem services.
     */
    fun start(
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket) -> Unit
    ) {
        this.localMeshId = localMeshId
        this.sendPacketFunc = sendPacketAction

        MeshLogger.i(TAG, "Starting Network Reliability & Self-Healing Subsystem for $localMeshId...")

        heartbeatManager.start(localMeshId, sendPacketAction)
        updateHealthMetricsSnapshot()
    }

    /**
     * Stops background timers and monitors.
     */
    fun stop() {
        heartbeatManager.stop()
        MeshLogger.i(TAG, "Stopped Network Reliability & Self-Healing Subsystem.")
    }

    /**
     * Central dispatcher for reliability control frames (HEARTBEAT, PARTITION_SYNC_MANIFEST, PARTITION_SYNC_REQUEST).
     */
    fun handleIncomingReliabilityPacket(
        packet: MeshPacket,
        immediateSender: String,
        sendDirectPacketAction: suspend (MeshPacket, String) -> Unit
    ): Boolean {
        when (packet.type) {
            PacketType.HEARTBEAT -> {
                val heartbeat = heartbeatManager.processHeartbeat(packet)
                if (heartbeat != null) {
                    healthManager.updateNodeConfidence(
                        nodeId = heartbeat.nodeId,
                        batteryLevel = heartbeat.batteryLevel,
                        lastSeenMs = heartbeat.timestamp,
                        isReachable = true
                    )
                }
                return true
            }

            PacketType.PARTITION_SYNC_MANIFEST -> {
                applicationScope.launch {
                    val pendingIds = storeAndForwardManager.getAllPendingIds().toSet()
                    partitionRecoveryManager.processSyncManifest(
                        packet = packet,
                        localMeshId = localMeshId,
                        localPendingIds = pendingIds,
                        sendPacketAction = sendDirectPacketAction
                    )
                }
                return true
            }

            PacketType.PARTITION_SYNC_REQUEST -> {
                // Peer requested missing packets for store-and-forward recovery
                storeAndForwardManager.flushPendingForPeer(packet.senderId) { reqPacket ->
                    sendDirectPacketAction(reqPacket, packet.senderId)
                }
                return true
            }

            else -> return false // Not a reliability control packet
        }
    }

    /**
     * Triggered when a peer establishes a physical connection.
     * Triggers partition reconnect sync and flushes store-and-forward queue.
     */
    fun onPeerConnected(peerId: String) {
        MeshLogger.i(TAG, "Peer connected: $peerId. Triggering partition recovery & store-and-forward flush...")
        healthManager.updateNodeConfidence(peerId, batteryLevel = 100, lastSeenMs = System.currentTimeMillis(), isReachable = true)
        transportHealthMonitor.updateAvailability(RouteType.BLE, true)

        val sendFunc = sendPacketFunc ?: return
        applicationScope.launch {
            val pendingIds = storeAndForwardManager.getAllPendingIds()
            partitionRecoveryManager.handlePeerReconnection(
                peerId = peerId,
                localMeshId = localMeshId,
                localPendingPacketIds = pendingIds,
                localRouteVersion = 1L,
                sendPacketAction = { packet, target -> sendFunc(packet) }
            )

            storeAndForwardManager.flushPendingForPeer(peerId, sendFunc)
            updateHealthMetricsSnapshot()
        }
    }

    /**
     * Triggered when a peer disconnects.
     */
    fun onPeerDisconnected(peerId: String) {
        MeshLogger.w(TAG, "Peer disconnected: $peerId")
        healthManager.updateNodeConfidence(peerId, batteryLevel = 0, lastSeenMs = System.currentTimeMillis(), isReachable = false)
        partitionRecoveryManager.recordPartitionSplit()
        updateHealthMetricsSnapshot()
    }

    /**
     * Triggered on topology / route updates to send event-driven heartbeats.
     */
    fun onRouteTableChanged() {
        heartbeatManager.incrementRouteVersion()
        sendPacketFunc?.let { action ->
            heartbeatManager.triggerEventDrivenHeartbeat(localMeshId, action)
        }
        updateHealthMetricsSnapshot()
    }

    /**
     * Records transmission delivery results to calculate confidence scores & health.
     */
    fun recordPacketTransmission(routeType: RouteType, latencyMs: Long, success: Boolean, rssi: Int = -70) {
        transportHealthMonitor.recordTransmission(routeType, latencyMs, success, rssi)
        updateHealthMetricsSnapshot()
    }

    fun updateHealthMetricsSnapshot() {
        val destinations = routeCache.getAllDestinations()
        val totalMeshSize = (destinations + localMeshId).distinct().size
        val connectedPeersCount = heartbeatManager.lastSeenPeers.size

        val routeConfidences = destinations.flatMap { dest ->
            routeCache.getRoutesForDestination(dest).map { route ->
                val nodeConf = healthManager.getNodeConfidence(route.nextHop)
                val transportConf = transportHealthMonitor.getConfidence(route.routeType)

                // Continuous confidence score formula
                val confidence = (nodeConf * 0.4f + (route.score / 100.0f) * 0.4f + transportConf * 0.2f).coerceIn(0.0f, 1.0f)

                RouteConfidence(
                    destinationId = route.destinationId,
                    nextHop = route.nextHop,
                    confidenceScore = confidence,
                    rssi = route.metrics.rssi,
                    averageLatencyMs = route.metrics.averageLatencyMs,
                    packetLossRate = route.metrics.packetLossRate,
                    hops = route.hops
                )
            }
        }

        healthManager.updateRouteConfidences(routeConfidences)

        val transports = transportHealthMonitor.transportHealth.value
        val bleMetrics = transports[RouteType.BLE]

        healthManager.updateMetricsSnapshot(
            meshSize = totalMeshSize,
            connectedPeersCount = connectedPeersCount,
            congestionLevel = congestionMonitor.congestionLevel.value,
            transports = transports,
            avgRtt = bleMetrics?.averageLatencyMs ?: 0L,
            lossRate = bleMetrics?.packetLossRate ?: 0.0f,
            retries = bleMetrics?.totalFailed ?: 0L,
            repairs = 0,
            discoveries = 0,
            queueSize = storeAndForwardManager.queuedCount.value,
            partitionSplits = partitionRecoveryManager.partitionEventCount.value,
            partitionHeals = partitionRecoveryManager.healEventCount.value,
            batteryImpact = if (congestionMonitor.isCongested()) "MODERATE" else "LOW"
        )
    }
}
