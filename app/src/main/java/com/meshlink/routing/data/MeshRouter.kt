package com.meshlink.routing.data

import androidx.annotation.VisibleForTesting
import com.meshlink.ble.api.BleTransport
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.util.MeshPacketParser
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.RelayPacketEntity
import com.meshlink.di.ApplicationScope
import com.meshlink.di.IoDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.recovery.engine.MeshReliabilityManager
import com.meshlink.routing.engine.RoutingEngine
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import com.meshlink.transport.HybridTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
internal class MeshRouter @Inject constructor(
    private val hybridTransport: HybridTransport,
    private val relayDao: RelayDao,
    private val trustManager: TrustManager,
    private val routingEngine: RoutingEngine,
    val reliabilityManager: MeshReliabilityManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : com.meshlink.routing.api.Router {

    companion object {
        private const val TAG = "MeshRouter"
        private const val MAX_RELAY_PACKETS = 1000
    }

    override var localMeshId: String = ""
        set(value) {
            field = value
            if (value.isNotBlank()) {
                reliabilityManager.start(value) { packet ->
                    hybridTransport.broadcastPacket(packet)
                }
            }
        }

    private val _incomingPayloads = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 200)
    override val incomingPayloads: SharedFlow<Pair<String, MeshPacket>> = _incomingPayloads.asSharedFlow()

    private val _packetEvents = MutableSharedFlow<com.meshlink.routing.api.PacketStatusEvent>(
        extraBufferCapacity = 1000,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val packetEvents: SharedFlow<com.meshlink.routing.api.PacketStatusEvent> = _packetEvents.asSharedFlow()

    override val routeTable: Map<String, com.meshlink.domain.model.RouteEntry>
        get() = routingEngine.routeManager.routeCache.getAllDestinations().mapNotNull { dest ->
            routingEngine.routeManager.getOptimalRoute(dest)?.let { dest to it }
        }.toMap()

    private var incomingJob: Job? = null
    private var storeForwardJob: Job? = null
    private var queueProcessorJob: Job? = null

    private val enforceEncryptionState = settingsRepository.advancedEncryptionEnforcement
        .stateIn(applicationScope, SharingStarted.Eagerly, true)

    private val relayEnabledState = settingsRepository.isMeshRelayEnabled
        .stateIn(applicationScope, SharingStarted.Eagerly, true)

    private val maxHopsState = settingsRepository.meshMaxHops
        .stateIn(applicationScope, SharingStarted.Eagerly, 5)

    private val meshTtlState = settingsRepository.meshTtl
        .stateIn(applicationScope, SharingStarted.Eagerly, 10)

    private val activeConnectedPeers = mutableSetOf<String>()

    init {
        observeIncoming()
        startStoreAndForwardLoop()
        startQueueProcessorLoop()
        routingEngine.start()
    }

    // ─────────────────── Incoming Observation ───────────────────

    @VisibleForTesting
    internal fun observeIncoming() {
        if (incomingJob?.isActive == true) return
        incomingJob = applicationScope.launch {
            hybridTransport.incomingPackets.collect { (sender, packet) ->
                try {
                    // Update connected peer set for reliability manager facade events
                    if (!activeConnectedPeers.contains(sender)) {
                        activeConnectedPeers.add(sender)
                        reliabilityManager.onPeerConnected(sender)
                    }

                    handleIncomingPacket(sender, packet)
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Error handling packet from $sender: ${e.message}")
                }
            }
        }
    }

    // ─────────────────── Store-and-Forward Loop ───────────────────

    @VisibleForTesting
    internal fun startStoreAndForwardLoop() {
        if (storeForwardJob?.isActive == true) return
        storeForwardJob = applicationScope.launch {
            while (isActive) {
                delay(30_000L)
                try {
                    tryDeliverCachedPackets()
                    relayDao.deleteExpiredPackets(System.currentTimeMillis())
                    relayDao.enforceStorageCap(MAX_RELAY_PACKETS)
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Store-and-forward loop error: ${e.message}")
                }
            }
        }
    }

    private suspend fun tryDeliverCachedPackets() {
        val cachedPackets = relayDao.getAllRelayPackets()
        if (cachedPackets.isEmpty()) return

        val connectedNodes = hybridTransport.connectedPeers
        if (connectedNodes.isEmpty()) return

        MeshLogger.d(TAG) { "S&F: attempting delivery of ${cachedPackets.size} cached packets to ${connectedNodes.size} peer(s)" }

        cachedPackets.forEach { entity ->
            routingEngine.congestionMonitor.decrementRelay()

            if (entity.ttl <= 0) {
                relayDao.deletePacket(entity.packetId)
                return@forEach
            }

            val targetTrustLevel = trustManager.getTrustLevel(entity.targetId)
            if (targetTrustLevel == TrustLevel.BLOCKED || targetTrustLevel == TrustLevel.REVOKED) {
                relayDao.deletePacket(entity.packetId)
                return@forEach
            }

            val packet = MeshPacket(
                packetId = entity.packetId,
                senderId = entity.senderId,
                targetId = entity.targetId,
                payload = entity.payload,
                type = try { PacketType.valueOf(entity.type) } catch (_: Exception) { PacketType.TEXT },
                priority = try { com.meshlink.domain.model.PacketPriority.valueOf(entity.priority) } catch (_: Exception) { com.meshlink.domain.model.PacketPriority.NORMAL },
                broadcastType = try { com.meshlink.domain.model.BroadcastType.valueOf(entity.broadcastType) } catch (_: Exception) { com.meshlink.domain.model.BroadcastType.NONE },
                transferId = entity.transferId,
                chunkIndex = entity.chunkIndex,
                totalChunks = entity.totalChunks,
                mimeType = entity.mimeType,
                encrypted = entity.encrypted,
                ttl = entity.ttl - 1,
                hopCount = entity.hopCount + 1,
                visitedPath = emptyList()
            )

            routingEngine.markPacketProcessed(packet.packetId)
            val nextHop = routingEngine.routeOptimizer.getLoadBalancedRoute(packet.targetId, emptySet())?.nextHop

            if (nextHop != null && connectedNodes.contains(nextHop)) {
                hybridTransport.broadcastPacket(packet, includeAddress = nextHop)
            } else {
                hybridTransport.broadcastPacket(packet)
            }

            relayDao.deletePacket(entity.packetId)
            MeshLogger.d(TAG) { "S&F: delivered ${com.meshlink.util.MeshIdNormalizer.canonicalize(entity.packetId)}" }
        }
    }

    // ─────────────────── Core Packet Handler ───────────────────

    private fun handleIncomingPacket(
        immediateSenderAddress: String,
        packet: MeshPacket
    ) {
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(packet.targetId)
        val canonicalLocalId = com.meshlink.util.MeshIdNormalizer.canonicalize(localMeshId)
        val isBroadcast = packet.targetId == "BROADCAST" || canonicalTargetId == "BROADCAST"
        val isForMe = canonicalTargetId.isNotBlank() && canonicalLocalId.isNotBlank() && canonicalTargetId == canonicalLocalId

        // Reliability control frames handler via MeshReliabilityManager facade
        if (reliabilityManager.handleIncomingReliabilityPacket(packet, immediateSenderAddress) { pkt, target ->
                hybridTransport.broadcastPacket(pkt, includeAddress = target)
            }) {
            return
        }

        // Encryption policy validation
        val enforceEncryption = enforceEncryptionState.value
        val isValid = com.meshlink.security.policy.PacketEncryptionPolicy.validatePacketEncryption(
            packet = packet,
            strictMode = enforceEncryption,
            hasSecureSession = false
        )

        if (!isValid) {
            MeshLogger.w(TAG, "Dropped unencrypted packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} due to Encryption policy")
            return
        }

        val trustLevel = trustManager.getTrustLevel(packet.senderId)
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Dropped packet from rogue node ${packet.senderId}")
            return
        }

        // Handle Routing Control Overhead Packets (RREQ, RREP, RERR)
        when (packet.type) {
            PacketType.ROUTE_REQUEST -> {
                applicationScope.launch {
                    routingEngine.discoveryEngine.handleRouteRequest(
                        immediateSenderAddress,
                        packet,
                        localMeshId,
                        sendPacketAction = { p, target -> hybridTransport.broadcastPacket(p, includeAddress = target) }
                    )
                }
                return
            }
            PacketType.ROUTE_REPLY -> {
                applicationScope.launch {
                    routingEngine.discoveryEngine.handleRouteReply(
                        immediateSenderAddress,
                        packet,
                        localMeshId,
                        sendPacketAction = { p, target -> hybridTransport.broadcastPacket(p, includeAddress = target) },
                        flushPendingAction = { targetId, pendingList ->
                            pendingList.forEach { queuedPacket ->
                                routingEngine.queueOptimizer.enqueue(queuedPacket)
                            }
                        }
                    )
                }
                return
            }
            PacketType.ROUTE_ERROR -> {
                routingEngine.repairManager.handleRouteError(
                    immediateSenderAddress,
                    packet,
                    localMeshId
                )
                return
            }
            else -> {}
        }

        // Strict deduplication
        val isDuplicate = !routingEngine.markPacketProcessed(packet.packetId)
        if (isDuplicate) {
            if (isForMe && packet.type != PacketType.DELIVERY_ACK) {
                MeshLogger.d(TAG) { "Dedup: re-processing duplicate ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} for local delivery/ACK" }
            } else {
                MeshLogger.d(TAG) { "Dedup: dropped duplicate ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}" }
                return
            }
        }

        // Dynamic Route Learning - Track sender's path
        routingEngine.routeManager.updateRoute(
            destinationId = packet.senderId,
            nextHop = immediateSenderAddress,
            hops = packet.hopCount,
            rssi = -65,
            trustScore = trustManager.getTrustScore(packet.senderId),
            type = RouteType.BLE
        )
        reliabilityManager.onRouteTableChanged()

        // Deliver locally if for us or broadcast
        if (isForMe || isBroadcast) {
            if (packet.type == PacketType.DELIVERY_ACK) {
                routingEngine.routeManager.recordDeliverySuccess(packet.senderId, immediateSenderAddress, 100L)
            }

            val emitted = _incomingPayloads.tryEmit(packet.senderId to packet)
            if (!emitted) {
                MeshLogger.w(TAG, "incomingPayloads buffer full — packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} dropped")
            }
        }

        if (isForMe) return

        val isAckNack = packet.type == PacketType.MEDIA_ACK || packet.type == PacketType.MEDIA_NACK
        if (packet.ttl <= 0) return

        if (routingEngine.isRoutingLoop(packet, localMeshId)) {
            MeshLogger.d(TAG) { "Loop guard: already visited or TTL expired ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}, dropping" }
            return
        }

        val relayEnabled = relayEnabledState.value
        if (!relayEnabled && !isAckNack) {
            MeshLogger.d(TAG) { "Relay disabled in settings, dropping packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}" }
            return
        }

        val maxHops = maxHopsState.value
        if (packet.hopCount >= maxHops) {
            MeshLogger.d(TAG) { "Max hops exceeded, dropping packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}" }
            return
        }

        val relayPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            visitedPath = if (localMeshId.isNotBlank()) packet.visitedPath + localMeshId else packet.visitedPath
        )

        val connectedNodes = hybridTransport.connectedPeers
        val hasPeersToForward = connectedNodes.any { it != immediateSenderAddress }

        if (routingEngine.congestionMonitor.isCongested() && !routingEngine.qosManager.shouldBypassQueue(packet.type)) {
            MeshLogger.w(TAG, "Congestion critical: delaying non-critical packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            if (!isAckNack) {
                storeForLater(relayPacket)
            }
            return
        }

        if (hasPeersToForward) {
            val nextHop = routingEngine.routeOptimizer.getLoadBalancedRoute(relayPacket.targetId, excludeHops = setOf(immediateSenderAddress))?.nextHop
            if (nextHop != null && connectedNodes.contains(nextHop)) {
                routingEngine.queueOptimizer.enqueue(relayPacket)
                MeshLogger.d(TAG) { "Directed load-balanced relay queued ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} via $nextHop" }
            } else {
                if (routingEngine.shouldRelayBroadcast(relayPacket.type)) {
                    routingEngine.congestionMonitor.recordBroadcast()
                    applicationScope.launch {
                        delay(kotlin.random.Random.nextLong(10L, 50L))
                        routingEngine.queueOptimizer.enqueue(relayPacket)
                    }
                    MeshLogger.d(TAG) { "Forwarded broadcast queued with jitter ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} (ttl=${relayPacket.ttl})" }
                }
            }
        } else if (!isAckNack) {
            storeForLater(relayPacket)
        }
    }

    private fun storeForLater(packet: MeshPacket) {
        applicationScope.launch {
            try {
                routingEngine.congestionMonitor.incrementRelay()
                reliabilityManager.storeAndForwardManager.enqueue(packet)
                MeshLogger.d(TAG) { "Stored ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} for later delivery" }
            } catch (e: Exception) {
                routingEngine.congestionMonitor.decrementRelay()
                MeshLogger.e(TAG, "Failed to cache relay packet: ${e.message}")
            }
        }
    }

    // ─────────────────── Send Methods ───────────────────

    @Deprecated("Use routePayload instead", ReplaceWith("routePayload(targetId, payload, myAddressAlias, encrypted, packetId)"))
    override fun sendPayload(
        targetId: String,
        payload: String,
        myAddressAlias: String,
        encrypted: Boolean,
        packetId: String?
    ) {
        val initialTtl = meshTtlState.value
        val packet = MeshPacket(
            packetId = packetId ?: java.util.UUID.randomUUID().toString(),
            senderId = myAddressAlias,
            targetId = targetId,
            payload = payload,
            encrypted = encrypted,
            ttl = initialTtl
        )

        routingEngine.markPacketProcessed(packet.packetId)
        routingEngine.queueOptimizer.enqueue(packet)
    }

    override suspend fun routePayload(
        targetId: String,
        payload: String,
        myAddressAlias: String,
        encrypted: Boolean,
        packetId: String?
    ): com.meshlink.domain.model.DispatchResult {
        return try {
            val initialTtl = meshTtlState.value
            val packet = MeshPacket(
                packetId = packetId ?: java.util.UUID.randomUUID().toString(),
                senderId = myAddressAlias,
                targetId = targetId,
                payload = payload,
                encrypted = encrypted,
                ttl = initialTtl
            )
            routingEngine.markPacketProcessed(packet.packetId)

            val canonicalTarget = com.meshlink.util.MeshIdNormalizer.canonicalize(targetId)
            val isBroadcast = canonicalTarget == "BROADCAST" || targetId == "BROADCAST"
            val knownRoute = routingEngine.routeOptimizer.getLoadBalancedRoute(targetId)

            if (!isBroadcast && knownRoute == null) {
                MeshLogger.d(TAG) { "No cached route for $targetId. Initiating RREQ discovery & queuing packet ${packet.packetId}" }
                routingEngine.discoveryEngine.queueAndDiscover(
                    targetId = targetId,
                    packet = packet,
                    localMeshId = localMeshId,
                    sendPacketAction = { hybridTransport.broadcastPacket(it) }
                )
            } else {
                routingEngine.queueOptimizer.enqueue(packet)
            }

            _packetEvents.emit(com.meshlink.routing.api.PacketQueued(packet.packetId))
            com.meshlink.domain.model.DispatchResult.Queued
        } catch (e: Exception) {
            com.meshlink.domain.model.DispatchResult.Error(e)
        }
    }

    @Deprecated("Use routeMediaPacket instead", ReplaceWith("routeMediaPacket(packet)"))
    override fun sendMediaPacket(packet: MeshPacket) {
        val initialTtl = meshTtlState.value
        val finalPacket = packet.copy(ttl = initialTtl)
        routingEngine.markPacketProcessed(finalPacket.packetId)
        routingEngine.queueOptimizer.enqueue(finalPacket)
    }

    override suspend fun routeMediaPacket(packet: MeshPacket): com.meshlink.domain.model.DispatchResult {
        return try {
            val initialTtl = meshTtlState.value
            val finalPacket = packet.copy(ttl = initialTtl)
            routingEngine.markPacketProcessed(finalPacket.packetId)
            routingEngine.queueOptimizer.enqueue(finalPacket)
            _packetEvents.emit(com.meshlink.routing.api.PacketQueued(packet.packetId))
            com.meshlink.domain.model.DispatchResult.Queued
        } catch (e: Exception) {
            com.meshlink.domain.model.DispatchResult.Error(e)
        }
    }

    // ─────────────────── Queue Processor ───────────────────

    @VisibleForTesting
    internal fun startQueueProcessorLoop() {
        if (queueProcessorJob?.isActive == true) return
        queueProcessorJob = applicationScope.launch {
            while (isActive) {
                if (routingEngine.queueOptimizer.size() == 0) {
                    delay(10)
                    continue
                }

                val packet = routingEngine.queueOptimizer.dequeue() ?: continue

                if (!routingEngine.retryEngine.shouldRetryNow() && packet.type != PacketType.SOS) {
                    routingEngine.queueOptimizer.enqueue(packet)
                    delay(500)
                    continue
                }

                val connectedNodes = hybridTransport.connectedPeers
                val nextHop = routingEngine.routeOptimizer.getLoadBalancedRoute(packet.targetId, excludeHops = emptySet())?.nextHop

                val startTime = System.currentTimeMillis()
                try {
                    if (packet.senderId == localMeshId) {
                        _packetEvents.emit(com.meshlink.routing.api.PacketTransmissionStarted(packet.packetId))
                    }

                    val sendResult = if (nextHop != null && connectedNodes.contains(nextHop)) {
                        hybridTransport.broadcastPacket(packet, includeAddress = nextHop)
                    } else {
                        hybridTransport.broadcastPacket(packet)
                    }

                    val latency = System.currentTimeMillis() - startTime
                    reliabilityManager.recordPacketTransmission(RouteType.BLE, latency, true)

                    if (packet.senderId == localMeshId) {
                        _packetEvents.emit(com.meshlink.routing.api.PacketTransmitted(packet.packetId))
                    }
                } catch (e: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    reliabilityManager.recordPacketTransmission(RouteType.BLE, latency, false)

                    MeshLogger.e(TAG, "Failed to send packet: ${e.message}")
                    if (packet.senderId == localMeshId) {
                        _packetEvents.emit(com.meshlink.routing.api.PacketFailed(packet.packetId, e))
                    }
                    storeForLater(packet)
                }

                if (routingEngine.congestionMonitor.isCongested()) {
                    delay(routingEngine.retryEngine.calculateRetryDelay(1))
                } else {
                    delay(5)
                }
            }
        }
    }
}