package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight AODV-inspired Route Discovery Engine.
 * Manages Route Request (RREQ) broadcasts, Route Reply (RREP) unicasts,
 * sequence number tracking, and pending packet queues waiting for route resolution.
 */
@Singleton
class RouteDiscoveryEngine @Inject constructor(
    private val routeCache: RouteCache,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "RouteDiscoveryEngine"
        private const val RREQ_TIMEOUT_MS = 2_000L
        private const val MAX_RREQ_ATTEMPTS = 3
    }

    private val localSequenceNumber = AtomicLong(1L)

    // Queued packets waiting for route resolution: TargetId -> List of packets
    private val pendingQueue = ConcurrentHashMap<String, MutableList<MeshPacket>>()
    private val pendingMutex = Mutex()

    // Active in-flight discoveries: TargetId -> DiscoveryJob
    private val activeDiscoveries = ConcurrentHashMap<String, Job>()

    private val _discoveryCount = MutableStateFlow(0)
    val discoveryCount: StateFlow<Int> = _discoveryCount.asStateFlow()

    private val _pendingQueueSize = MutableStateFlow(0)
    val pendingQueueSize: StateFlow<Int> = _pendingQueueSize.asStateFlow()

    fun getNextSequenceNumber(): Long = localSequenceNumber.incrementAndGet()

    /**
     * Initiates route discovery for [targetId]. If a discovery is already running for [targetId],
     * queues [packet] to wait for completion instead of broadcasting duplicate RREQs.
     */
    fun queueAndDiscover(
        targetId: String,
        packet: MeshPacket?,
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket) -> Unit
    ) {
        applicationScope.launch {
            if (packet != null) {
                pendingMutex.withLock {
                    val list = pendingQueue.getOrPut(targetId) { mutableListOf() }
                    list.add(packet)
                    _pendingQueueSize.value = pendingQueue.values.sumOf { it.size }
                }
            }

            // Deduplicate active discoveries
            if (activeDiscoveries.containsKey(targetId)) {
                MeshLogger.d(TAG) { "Discovery already active for $targetId, queued packet and waiting." }
                return@launch
            }

            val job = applicationScope.launch {
                try {
                    _discoveryCount.value = activeDiscoveries.size + 1
                    var attempts = 0
                    var routeFound = false

                    while (attempts < MAX_RREQ_ATTEMPTS && !routeFound && isActive) {
                        attempts++
                        val seqNum = getNextSequenceNumber()
                        val rreqPacket = MeshPacket(
                            packetId = UUID.randomUUID().toString(),
                            senderId = localMeshId,
                            targetId = targetId,
                            payload = "RREQ:$targetId",
                            type = PacketType.ROUTE_REQUEST,
                            priority = PacketPriority.HIGH,
                            ttl = 10,
                            hopCount = 0,
                            visitedPath = listOf(localMeshId),
                            sequenceNumber = seqNum
                        )

                        MeshLogger.d(TAG) { "Broadcasting RREQ attempt $attempts/$MAX_RREQ_ATTEMPTS for $targetId (seq=$seqNum)" }
                        sendPacketAction(rreqPacket)

                        // Wait for reply or timeout
                        val checkInterval = 100L
                        var elapsed = 0L
                        while (elapsed < RREQ_TIMEOUT_MS && isActive) {
                            delay(checkInterval)
                            elapsed += checkInterval
                            val route = routeCache.getRoutesForDestination(targetId).firstOrNull()
                            if (route != null) {
                                routeFound = true
                                break
                            }
                        }
                    }

                    if (routeFound) {
                        MeshLogger.d(TAG) { "Route discovery succeeded for $targetId!" }
                    } else {
                        MeshLogger.w(TAG, "Route discovery timed out after $MAX_RREQ_ATTEMPTS attempts for $targetId")
                    }
                } finally {
                    activeDiscoveries.remove(targetId)
                    _discoveryCount.value = activeDiscoveries.size
                }
            }

            activeDiscoveries[targetId] = job
        }
    }

    /**
     * Handles an incoming RREQ.
     * 1. Records reverse route back to RREQ source.
     * 2. If local device is the destination, sends RREP unicast back along the reverse path.
     * 3. Else, forwards RREQ broadcast if not already visited.
     */
    suspend fun handleRouteRequest(
        immediateSender: String,
        packet: MeshPacket,
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket, String?) -> Unit
    ) {
        val sourceId = packet.senderId
        val targetId = packet.targetId

        // Reverse Route Learning
        val existingRoute = routeCache.getRoutesForDestination(sourceId).find { it.nextHop == immediateSender }
        if (existingRoute == null || packet.sequenceNumber > existingRoute.sequenceNumber || packet.hopCount < existingRoute.hops) {
            val route = RouteEntry(
                destinationId = sourceId,
                nextHop = immediateSender,
                hops = packet.hopCount,
                routeType = RouteType.BLE,
                lastSeen = System.currentTimeMillis(),
                sequenceNumber = packet.sequenceNumber
            )
            routeCache.addOrUpdateRoute(route)
            MeshLogger.d(TAG) { "Learned reverse route to $sourceId via $immediateSender (hops=${packet.hopCount})" }
        }

        // Am I the target destination?
        if (localMeshId.isNotBlank() && targetId == localMeshId) {
            val rrepSeqNum = getNextSequenceNumber()
            val rrepPacket = MeshPacket(
                packetId = UUID.randomUUID().toString(),
                senderId = localMeshId,
                targetId = sourceId,
                payload = "RREP:$localMeshId",
                type = PacketType.ROUTE_REPLY,
                priority = PacketPriority.HIGH,
                ttl = 10,
                hopCount = 0,
                visitedPath = listOf(localMeshId),
                sequenceNumber = rrepSeqNum,
                sourceSequenceNumber = packet.sequenceNumber
            )
            MeshLogger.d(TAG) { "Local target reached! Sending RREP unicast back to $sourceId via $immediateSender" }
            sendPacketAction(rrepPacket, immediateSender)
        } else {
            // Forward RREQ broadcast
            if (packet.ttl > 1 && !packet.visitedPath.contains(localMeshId)) {
                val updatedRreq = packet.copy(
                    ttl = packet.ttl - 1,
                    hopCount = packet.hopCount + 1,
                    visitedPath = packet.visitedPath + localMeshId
                )
                MeshLogger.d(TAG) { "Forwarding RREQ for $targetId (ttl=${updatedRreq.ttl})" }
                sendPacketAction(updatedRreq, null)
            }
        }
    }

    /**
     * Handles an incoming RREP.
     * 1. Records forward route to RREP target (the node responding).
     * 2. If RREP is meant for us, flushes pending packet queue.
     * 3. Else, forwards RREP unicast to next hop towards the original RREQ source.
     */
    suspend fun handleRouteReply(
        immediateSender: String,
        packet: MeshPacket,
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket, String?) -> Unit,
        flushPendingAction: suspend (String, List<MeshPacket>) -> Unit
    ) {
        val responderId = packet.senderId
        val destinationForRrep = packet.targetId

        // Forward Route Learning: We now have a path to responderId via immediateSender
        val route = RouteEntry(
            destinationId = responderId,
            nextHop = immediateSender,
            hops = packet.hopCount,
            routeType = RouteType.BLE,
            lastSeen = System.currentTimeMillis(),
            sequenceNumber = packet.sequenceNumber
        )
        routeCache.addOrUpdateRoute(route)
        MeshLogger.d(TAG) { "Learned forward route to $responderId via $immediateSender (hops=${packet.hopCount})" }

        if (localMeshId.isNotBlank() && destinationForRrep == localMeshId) {
            // RREP reached origin source! Flush pending packets
            val packetsToFlush = pendingMutex.withLock {
                pendingQueue.remove(responderId) ?: emptyList()
            }
            _pendingQueueSize.value = pendingQueue.values.sumOf { it.size }

            if (packetsToFlush.isNotEmpty()) {
                MeshLogger.d(TAG) { "RREP received! Flushing ${packetsToFlush.size} pending packet(s) to $responderId" }
                flushPendingAction(responderId, packetsToFlush)
            }
        } else {
            // Forward RREP unicast towards destinationForRrep (the RREQ origin source)
            val nextHopToOrigin = routeCache.getRoutesForDestination(destinationForRrep)
                .firstOrNull()?.nextHop

            if (nextHopToOrigin != null && packet.ttl > 1) {
                val updatedRrep = packet.copy(
                    ttl = packet.ttl - 1,
                    hopCount = packet.hopCount + 1,
                    visitedPath = packet.visitedPath + localMeshId
                )
                MeshLogger.d(TAG) { "Forwarding RREP for $responderId to origin $destinationForRrep via $nextHopToOrigin" }
                sendPacketAction(updatedRrep, nextHopToOrigin)
            } else {
                MeshLogger.w(TAG, "Cannot forward RREP to origin $destinationForRrep: no next hop found")
            }
        }
    }

    /**
     * Clears all pending queue items.
     */
    fun clearPending() {
        applicationScope.launch {
            pendingMutex.withLock {
                pendingQueue.clear()
                _pendingQueueSize.value = 0
            }
        }
    }
}
