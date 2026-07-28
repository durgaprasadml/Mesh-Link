package com.meshlink.routing.data

import com.meshlink.common.logger.MeshLogger

import com.meshlink.ble.api.BleTransport
import androidx.annotation.VisibleForTesting
import com.meshlink.domain.model.MeshPacket
import com.meshlink.common.util.MeshPacketParser
import com.meshlink.domain.model.PacketType
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.RelayPacketEntity
import com.meshlink.di.IoDispatcher
import com.meshlink.routing.engine.RoutingEngine
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.RouteType
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import com.meshlink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn


@Singleton
internal class MeshRouter @Inject constructor(
    private val bleTransport: BleTransport,

    private val relayDao: RelayDao,
    private val trustManager: TrustManager,
    private val routingEngine: RoutingEngine,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : com.meshlink.routing.api.Router {

    companion object {
        private const val TAG = "MeshRouter"
        private const val RECONNECT_INTERVAL_MS = 10_000L
        private const val MAX_RELAY_PACKETS = 1000
    }

    override var localMeshId: String = ""

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
            bleTransport.incomingPackets.collect { (sender, packet) ->
                MeshLogger.d(TAG, "[TRANSPORT-B] ═══ MeshRouter.observeIncoming() ═══")
                MeshLogger.d(TAG, "[TRANSPORT-B]   immediateSender : '$sender'")
                MeshLogger.d(TAG, "[TRANSPORT-B]   ✓ Packet received directly:")
                MeshLogger.d(TAG, "[TRANSPORT-B]     packetId  : '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
                MeshLogger.d(TAG, "[TRANSPORT-B]     senderId  : '${packet.senderId}'")
                MeshLogger.d(TAG, "[TRANSPORT-B]     targetId  : '${packet.targetId}'")
                MeshLogger.d(TAG, "[TRANSPORT-B]     type      : '${packet.type}'")
                MeshLogger.d(TAG, "[TRANSPORT-B]     encrypted : ${packet.encrypted}")
                MeshLogger.d(TAG, "[TRANSPORT-B]     ttl       : ${packet.ttl}")
                try {
                    handleIncomingPacket(sender, packet)
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Error handling BLE packet from $sender: ${e.message}")
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

        val connectedNodes = bleTransport.connectedPeers
        if (connectedNodes.isEmpty()) {
            return
        }

        MeshLogger.d(TAG, "S&F: attempting delivery of ${cachedPackets.size} cached packets to ${connectedNodes.size} peer(s)")

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

            val json = MeshPacketParser.toJson(packet)
            
            // Re-evaluate next hop upon S&F un-queueing
            val nextHop = routingEngine.getNextHopForForwarding(packet, connectedNodes, "")
            
            if (nextHop != null) {
                bleTransport.broadcast(packet, includeAddress = nextHop)
            } else {
                bleTransport.broadcast(packet)
            }
            
            relayDao.deletePacket(entity.packetId)
            MeshLogger.d(TAG, "S&F: delivered ${com.meshlink.util.MeshIdNormalizer.canonicalize(entity.packetId)}")
        }
    }

    // ─────────────────── Core Packet Handler ───────────────────

    private fun handleIncomingPacket(
        immediateSenderAddress: String,
        packet: MeshPacket
    ) {
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(packet.targetId)
        val canonicalLocalId  = com.meshlink.util.MeshIdNormalizer.canonicalize(localMeshId)
        val isBroadcast = packet.targetId == "BROADCAST" || canonicalTargetId == "BROADCAST"
        val isForMe     = canonicalTargetId.isNotBlank() && canonicalLocalId.isNotBlank() && canonicalTargetId == canonicalLocalId

        // --- Strict Encryption Enforcement ---
        val enforceEncryption = enforceEncryptionState.value
        val isValid = com.meshlink.security.policy.PacketEncryptionPolicy.validatePacketEncryption(
            packet = packet,
            strictMode = enforceEncryption,
            hasSecureSession = false // Relaying nodes don't check destination session state; local delivery is fully validated post-decryption in dispatcher.
        )

        if (!isValid) {
            MeshLogger.w(TAG, "Dropped unencrypted packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} due to Encryption policy")
            return
        }

        // --- Trust Validation ---
        val trustLevel = trustManager.getTrustLevel(packet.senderId)
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Dropped packet from rogue node ${packet.senderId}")
            return
        }

        // ── DIAGNOSTIC Stage 3 (PRIMARY KILL SWITCH) ─────────────────────────
        MeshLogger.d(TAG, "[DIAG-Stage3] ═══ MeshRouter.handleIncomingPacket() ═══")
        MeshLogger.d(TAG, "[DIAG-Stage3]   packet.packetId (last-6) : '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
        MeshLogger.d(TAG, "[DIAG-Stage3]   packet.type              : '${packet.type}'")
        MeshLogger.d(TAG, "[DIAG-Stage3]   packet.senderId          : '${packet.senderId}'")
        MeshLogger.d(TAG, "[DIAG-Stage3]   packet.targetId          : '${packet.targetId}' (norm: '$canonicalTargetId')")
        MeshLogger.d(TAG, "[DIAG-Stage3]   localMeshId              : '$localMeshId' (norm: '$canonicalLocalId')")
        MeshLogger.d(TAG, "[DIAG-Stage3]   isBroadcast              : $isBroadcast")
        MeshLogger.d(TAG, "[DIAG-Stage3]   isForMe                  : $isForMe")
        if (!isForMe && !isBroadcast) {
            MeshLogger.w(TAG, "[DIAG-Stage3]   ⚠ isForMe=false AND isBroadcast=false")
            MeshLogger.w(TAG, "[DIAG-Stage3]   ⚠ Packet will NOT be emitted to _incomingPayloads")
        } else {
            MeshLogger.d(TAG, "[DIAG-Stage3]   ✓ Packet will be emitted to _incomingPayloads (isForMe=$isForMe isBroadcast=$isBroadcast)")
        }
        // ─────────────────────────────────────────────────────────────────────

        // Strict de-dup — reject if already processed, UNLESS it's a direct message for us
        // (we want to re-process duplicates for ourselves so we can re-send ACKs if the sender retried)
        val isDuplicate = !routingEngine.markPacketProcessed(packet.packetId)
        if (isDuplicate) {
            if (isForMe && packet.type != PacketType.DELIVERY_ACK) {
                MeshLogger.d(TAG, "Dedup: re-processing duplicate ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} for local delivery/ACK")
            } else {
                MeshLogger.d(TAG, "Dedup: dropped duplicate ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
                return
            }
        }

        // Dynamic Route Learning - Track this sender's path
        routingEngine.routeManager.updateRoute(
            destinationId = packet.senderId,
            nextHop = immediateSenderAddress,
            hops = packet.hopCount,
            rssi = -65, // In the future, we could extract RSSI from BLE stack for this packet, but for now just update freshness
            trustScore = trustManager.getTrustScore(packet.senderId),
            type = RouteType.BLE
        )

        MeshLogger.d(TAG, "Packet [${packet.type}] from=${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.senderId)} target=${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.targetId)} ttl=${packet.ttl} hops=${packet.hopCount}")



        // Deliver locally if it's for us or a broadcast
        if (isForMe || isBroadcast) {

            
            // If it's a delivery ACK, we can record a successful delivery on our route
            if (packet.type == PacketType.DELIVERY_ACK) {
                // The payload contains the packet ID that was delivered.
                // We'd need to track latency, but for now we'll just track success.
                routingEngine.routeManager.recordDeliverySuccess(packet.senderId, immediateSenderAddress, 100L)
            }
            
            val emitted = _incomingPayloads.tryEmit(packet.senderId to packet)
            if (!emitted) {
                MeshLogger.w(TAG, "incomingPayloads buffer full — packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} dropped")
            } else {
                MeshLogger.d(TAG, "[DIAG-Stage3]   ✓ _incomingPayloads.tryEmit() succeeded for ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            }
        }

        // Packets FOR US: do not forward or store
        if (isForMe) return

        // ACK/NACK are ephemeral
        val isAckNack = packet.type == PacketType.MEDIA_ACK || packet.type == PacketType.MEDIA_NACK

        // TTL check
        if (packet.ttl <= 0) return

        // Loop guard
        if (routingEngine.isRoutingLoop(packet, localMeshId)) {
            MeshLogger.d(TAG, "Loop guard: already visited or TTL expired ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}, dropping")
            return
        }

        // Check Mesh Relay setting
        val relayEnabled = relayEnabledState.value
        if (!relayEnabled && !isAckNack) {
            MeshLogger.d(TAG, "Relay disabled in settings, dropping packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            return
        }

        val maxHops = maxHopsState.value
        if (packet.hopCount >= maxHops) {
            MeshLogger.d(TAG, "Max hops exceeded, dropping packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            return
        }

        val relayPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            visitedPath = if (localMeshId.isNotBlank()) packet.visitedPath + localMeshId else packet.visitedPath
        )



        val forwardedJson = MeshPacketParser.toJson(relayPacket)
        val connectedNodes = bleTransport.connectedPeers
        val hasPeersToForward = connectedNodes.any { it != immediateSenderAddress }

        // Congestion Check
        if (routingEngine.congestionMonitor.isCongested() && !routingEngine.qosManager.shouldBypassQueue(packet.type)) {
            MeshLogger.w(TAG, "Congestion critical: dropping/delaying non-critical packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            if (!isAckNack) {
                storeForLater(relayPacket)
            }
            return
        }

        if (hasPeersToForward) {
            val nextHop = routingEngine.getNextHopForForwarding(relayPacket, connectedNodes, excludeHop = immediateSenderAddress)
            if (nextHop != null) {
                routingEngine.queueOptimizer.enqueue(relayPacket)
                MeshLogger.d(TAG, "Directed relay queued ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} via $nextHop")
            } else {
                if (routingEngine.shouldRelayBroadcast(relayPacket.type)) {
                    routingEngine.congestionMonitor.recordBroadcast()
                    routingEngine.queueOptimizer.enqueue(relayPacket)
                    MeshLogger.d(TAG, "Forwarded broadcast queued ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} (ttl=${relayPacket.ttl})")
                } else {
                    MeshLogger.d(TAG, "Dropped broadcast due to battery/congestion heuristics")
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
                relayDao.insertPacket(
                    RelayPacketEntity(
                        packetId    = packet.packetId,
                        senderId    = packet.senderId,
                        targetId    = packet.targetId,
                        payload     = packet.payload,
                        type        = packet.type.name,
                        priority    = packet.priority.name,
                        broadcastType = packet.broadcastType.name,
                        ttl         = packet.ttl,
                        hopCount    = packet.hopCount,
                        encrypted   = packet.encrypted,
                        transferId  = packet.transferId,
                        chunkIndex  = packet.chunkIndex,
                        totalChunks = packet.totalChunks,
                        mimeType    = packet.mimeType
                    )
                )
                MeshLogger.d(TAG, "Stored ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} for later delivery")
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

        val serialized = MeshPacketParser.toJson(packet)
        MeshLogger.d(TAG, "[TRANSPORT-A] ═══ Packet Created & Enqueued ═══")
        MeshLogger.d(TAG, "[TRANSPORT-A]   packetId  : '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
        
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
            routingEngine.queueOptimizer.enqueue(packet)
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
                    delay(10) // Idle sleep
                    continue
                }

                val packet = routingEngine.queueOptimizer.dequeue() ?: continue

                if (!routingEngine.retryEngine.shouldRetryNow() && packet.type != PacketType.SOS) {
                    // Requeue if critically congested, but allow SOS
                    routingEngine.queueOptimizer.enqueue(packet)
                    delay(500)
                    continue
                }

                val json = MeshPacketParser.toJson(packet)
                val connectedNodes = bleTransport.connectedPeers
                val nextHop = routingEngine.getNextHopForForwarding(packet, connectedNodes, excludeHop = "")

                // ── [TRANSPORT-A] Queue Dequeue & Dispatch ────────────────────────────────
                MeshLogger.d(TAG, "[TRANSPORT-A] ═══ Queue Processor: Dequeued Packet ═══")
                MeshLogger.d(TAG, "[TRANSPORT-A]   packetId    : '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
                MeshLogger.d(TAG, "[TRANSPORT-A]   senderId    : '${packet.senderId}'")
                MeshLogger.d(TAG, "[TRANSPORT-A]   targetId    : '${packet.targetId}'")
                MeshLogger.d(TAG, "[TRANSPORT-A]   type        : '${packet.type}'")
                MeshLogger.d(TAG, "[TRANSPORT-A]   serializedBytes : ${json.toByteArray(Charsets.UTF_8).size} B")
                MeshLogger.d(TAG, "[TRANSPORT-A]   connectedNodes  : ${connectedNodes.size}  -> $connectedNodes")
                MeshLogger.d(TAG, "[TRANSPORT-A]   nextHop         : '${nextHop ?: "BROADCAST (no directed route)"}'")
                // ─────────────────────────────────────────────────────────────────────

                try {
                    // Emit transmission started event
                    if (packet.senderId == localMeshId) {
                        _packetEvents.emit(com.meshlink.routing.api.PacketTransmissionStarted(packet.packetId))
                    }
                    
                    // Check Intelligent Transport (Wi-Fi vs BLE)
                    val preferredTransport = routingEngine.transportManager.selectTransportForPayload(packet.targetId, packet.type)

                    // ── [TRANSPORT-A] Transport Selection ───────────────────────────────────
                    MeshLogger.d(TAG, "[TRANSPORT-A]   preferredTransport : $preferredTransport")

                    MeshLogger.d(TAG, "[TRANSPORT-A]   Transport = BLE")

                    if (nextHop != null) {
                        MeshLogger.d(TAG, "[TRANSPORT-A]   ▶ Calling bleTransport.broadcast(includeAddress='$nextHop') -- DIRECTED")
                        bleTransport.broadcast(packet, includeAddress = nextHop)
                    } else {
                        MeshLogger.d(TAG, "[TRANSPORT-A]   ▶ Calling bleTransport.broadcast() -- BROADCAST to ALL (${connectedNodes.size} nodes)")
                        bleTransport.broadcast(packet)
                    }
                    
                    // Emit transmitted event if originating locally
                    if (packet.senderId == localMeshId) {
                        _packetEvents.emit(com.meshlink.routing.api.PacketTransmitted(packet.packetId))
                    }
                    // ────────────────────────────────────────────────────────────────────
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "[TRANSPORT-A]   ✗ EXCEPTION sending packet: ${e.message}")
                    MeshLogger.e(TAG, "Failed to send packet: ${e.message}")
                    if (packet.senderId == localMeshId) {
                        _packetEvents.emit(com.meshlink.routing.api.PacketFailed(packet.packetId, e))
                    }
                    storeForLater(packet)
                }

                // If congested, add artificial delay (backoff) to pace the network
                if (routingEngine.congestionMonitor.isCongested()) {
                    delay(routingEngine.retryEngine.calculateRetryDelay(0))
                } else {
                    delay(5) // Minimal pacing to prevent BLE buffer overflows
                }
            }
        }
    }
}