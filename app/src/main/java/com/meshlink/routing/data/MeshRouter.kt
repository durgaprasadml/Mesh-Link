package com.meshlink.routing.data

import com.meshlink.common.logger.MeshLogger

import com.meshlink.ble.data.BleGattManager
import com.meshlink.domain.model.MeshPacket
import com.meshlink.ble.data.MeshPacketParser
import com.meshlink.domain.model.PacketType
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.RelayPacketEntity
import com.meshlink.di.IoDispatcher
import com.meshlink.routing.engine.RoutingEngine
import com.meshlink.routing.engine.RouteType
import com.meshlink.security.data.TrustLevel
import com.meshlink.security.data.TrustManager
import com.meshlink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

@Singleton
class MeshRouter @Inject constructor(
    private val gattManager: BleGattManager,

    private val relayDao: RelayDao,
    private val trustManager: TrustManager,
    private val routingEngine: RoutingEngine,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "MeshRouter"
        private const val RECONNECT_INTERVAL_MS = 10_000L
        private const val MAX_RELAY_PACKETS = 1000
    }

    var localMeshId: String = ""

    private val _incomingPayloads = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 200)
    val incomingPayloads: SharedFlow<Pair<String, MeshPacket>> = _incomingPayloads.asSharedFlow()

    val routeTable: Map<String, com.meshlink.routing.engine.RouteEntry>
        get() = routingEngine.routeManager.routeCache.getAllDestinations().mapNotNull { dest ->
            routingEngine.routeManager.getOptimalRoute(dest)?.let { dest to it }
        }.toMap()

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        observeIncoming()
        startReconnectLoop()
        startStoreAndForwardLoop()
        startQueueProcessorLoop()
        routingEngine.start()
    }

    // ─────────────────── Incoming Observation ───────────────────

    private fun observeIncoming() {
        scope.launch {
            gattManager.incomingMessages.collect { (sender, json) ->
                // ── [TRANSPORT-B] Packet received from BLE, about to parse ─────────────────────────
                MeshLogger.d(TAG, "[TRANSPORT-B] ═══ MeshRouter.observeIncoming() ═══")
                MeshLogger.d(TAG, "[TRANSPORT-B]   immediateSender : '$sender'")
                MeshLogger.d(TAG, "[TRANSPORT-B]   rawJsonBytes    : ${json.toByteArray(Charsets.UTF_8).size} B")
                MeshLogger.d(TAG, "[TRANSPORT-B]   jsonPreview     : '${json.take(100)}'")
                val parsed = com.meshlink.ble.data.MeshPacketParser.fromJson(json)
                if (parsed == null) {
                    MeshLogger.e(TAG, "[TRANSPORT-B]   ✗ MeshPacketParser.fromJson() returned null — JSON is malformed or empty")
                } else {
                    MeshLogger.d(TAG, "[TRANSPORT-B]   ✓ Packet parsed:")
                    MeshLogger.d(TAG, "[TRANSPORT-B]     packetId  : '${com.meshlink.util.MeshIdNormalizer.canonicalize(parsed.packetId)}'")
                    MeshLogger.d(TAG, "[TRANSPORT-B]     senderId  : '${parsed.senderId}'")
                    MeshLogger.d(TAG, "[TRANSPORT-B]     targetId  : '${parsed.targetId}'")
                    MeshLogger.d(TAG, "[TRANSPORT-B]     type      : '${parsed.type}'")
                    MeshLogger.d(TAG, "[TRANSPORT-B]     encrypted : ${parsed.encrypted}")
                    MeshLogger.d(TAG, "[TRANSPORT-B]     ttl       : ${parsed.ttl}")
                }
                // ───────────────────────────────────────────────────────────────────────
                try {
                    handleIncomingPacket(sender, json)
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Error handling BLE packet from $sender: ${e.message}")
                }
            }
        }
    }

    // ─────────────────── Store-and-Forward Loop ───────────────────

    private fun startStoreAndForwardLoop() {
        scope.launch {
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

        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
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
                visitedPath = mutableListOf()
            )

            routingEngine.markPacketProcessed(packet.packetId)

            val json = MeshPacketParser.toJson(packet)
            
            // Re-evaluate next hop upon S&F un-queueing
            val nextHop = routingEngine.getNextHopForForwarding(packet, connectedNodes, "")
            
            if (nextHop != null) {
                gattManager.broadcastPacket(json, includeAddress = nextHop)
            } else {
                gattManager.broadcastPacket(json)
            }
            
            relayDao.deletePacket(entity.packetId)
            MeshLogger.d(TAG, "S&F: delivered ${com.meshlink.util.MeshIdNormalizer.canonicalize(entity.packetId)}")
        }
    }

    // ─────────────────── Reconnect Loop ───────────────────

    private fun startReconnectLoop() {
        scope.launch {
            while (isActive) {
                delay(RECONNECT_INTERVAL_MS)
                val dests = routingEngine.routeManager.routeCache.getAllDestinations()
                if (dests.isNotEmpty() && gattManager.activeClients.isEmpty() && gattManager.connectedServers.isEmpty()) {
                    MeshLogger.d(TAG, "Reconnect loop: trying to re-establish mesh links.")
                    // Simply grab the best possible route to anyone
                    val bestRoute = dests.mapNotNull { routingEngine.routeManager.getOptimalRoute(it) }
                                         .maxByOrNull { it.score }
                    
                    if (bestRoute != null) {
                        try {
                            gattManager.connectToDevice(bestRoute.nextHop)
                        } catch (e: Exception) {
                            MeshLogger.w(TAG, "Reconnect failed for ${bestRoute.nextHop}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // ─────────────────── Core Packet Handler ───────────────────

    private fun handleIncomingPacket(
        immediateSenderAddress: String,
        json: String
    ) {
        if (json.isBlank() || !json.trimStart().startsWith("{")) {
            return
        }

        val packet = MeshPacketParser.fromJson(json) ?: return

        // --- Strict Encryption Enforcement ---
        val enforceEncryption = runBlocking { settingsRepository.advancedEncryptionEnforcement.first() }
        if (enforceEncryption && !packet.encrypted && packet.type != PacketType.KEY_EXCHANGE && packet.type != PacketType.SOS) {
            MeshLogger.w(TAG, "Dropped unencrypted packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)} due to Strict Encryption policy")
            return
        }

        // --- Trust Validation ---
        val trustLevel = trustManager.getTrustLevel(packet.senderId)
        if (trustLevel == TrustLevel.BLOCKED || trustLevel == TrustLevel.REVOKED) {
            MeshLogger.w(TAG, "Dropped packet from rogue node ${packet.senderId}")
            return
        }

        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(packet.targetId)
        val canonicalLocalId  = com.meshlink.util.MeshIdNormalizer.canonicalize(localMeshId)
        val isBroadcast = packet.targetId == "BROADCAST" || canonicalTargetId == "BROADCAST"
        val isForMe     = canonicalTargetId.isNotBlank() && canonicalLocalId.isNotBlank() && canonicalTargetId == canonicalLocalId

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
            MeshLogger.d(TAG, "Loop guard: already visited ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}, dropping")
            return
        }

        // Check Mesh Relay setting
        val relayEnabled = runBlocking { settingsRepository.isMeshRelayEnabled.first() }
        if (!relayEnabled && !isAckNack) {
            MeshLogger.d(TAG, "Relay disabled in settings, dropping packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            return
        }

        val maxHops = runBlocking { settingsRepository.meshMaxHops.first() }
        if (packet.hopCount >= maxHops) {
            MeshLogger.d(TAG, "Max hops exceeded, dropping packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
            return
        }

        val relayPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            visitedPath = (packet.visitedPath.toMutableList().also {
                if (localMeshId.isNotBlank()) it.add(localMeshId)
            })
        )



        val forwardedJson = MeshPacketParser.toJson(relayPacket)
        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
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
        scope.launch {
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

    fun sendPayload(
        targetId: String,
        payload: String,
        myAddressAlias: String = "Me",
        encrypted: Boolean = false,
        packetId: String? = null
    ) {
        val initialTtl = runBlocking { settingsRepository.meshTtl.first() }
        val packet = MeshPacket(
            packetId = packetId ?: java.util.UUID.randomUUID().toString(),
            senderId = myAddressAlias,
            targetId = targetId,
            payload = payload,
            encrypted = encrypted,
            ttl = initialTtl
        )

        // ── [TRANSPORT-A] Packet Created ──────────────────────────────────────────────────
        val serialized = MeshPacketParser.toJson(packet)
        MeshLogger.d(TAG, "[TRANSPORT-A] ═══ Packet Created & Enqueued ═══")
        MeshLogger.d(TAG, "[TRANSPORT-A]   packetId  : '${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}'")
        MeshLogger.d(TAG, "[TRANSPORT-A]   senderId  : '${packet.senderId}'")
        MeshLogger.d(TAG, "[TRANSPORT-A]   targetId  : '${packet.targetId}'")
        MeshLogger.d(TAG, "[TRANSPORT-A]   encrypted : ${packet.encrypted}")
        MeshLogger.d(TAG, "[TRANSPORT-A]   ttl       : ${packet.ttl}")
        MeshLogger.d(TAG, "[TRANSPORT-A]   serializedBytes : ${serialized.toByteArray(Charsets.UTF_8).size} B")
        MeshLogger.d(TAG, "[TRANSPORT-A]   payload preview : '${packet.payload.take(60)}'")
        // ────────────────────────────────────────────────────────────────────────

        routingEngine.markPacketProcessed(packet.packetId)

        routingEngine.queueOptimizer.enqueue(packet)
    }

    fun sendMediaPacket(packet: MeshPacket) {
        val initialTtl = runBlocking { settingsRepository.meshTtl.first() }
        val finalPacket = packet.copy(ttl = initialTtl)
        
        routingEngine.markPacketProcessed(finalPacket.packetId)

        routingEngine.queueOptimizer.enqueue(finalPacket)
    }



    // ─────────────────── Queue Processor ───────────────────

    private fun startQueueProcessorLoop() {
        scope.launch {
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
                val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
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
                    // Check Intelligent Transport (Wi-Fi vs BLE)
                    val preferredTransport = routingEngine.transportManager.selectTransportForPayload(packet.targetId, packet.type)

                    // ── [TRANSPORT-A] Transport Selection ───────────────────────────────────
                    MeshLogger.d(TAG, "[TRANSPORT-A]   preferredTransport : $preferredTransport")

                    if (preferredTransport == RouteType.WIFI_DIRECT) {
                        MeshLogger.d(TAG, "[TRANSPORT-A]   Transport = WIFI_DIRECT (fallback to BLE if socket not ready)")
                        MeshLogger.d(TAG, "Preferred transport is Wi-Fi Direct for packet ${com.meshlink.util.MeshIdNormalizer.canonicalize(packet.packetId)}")
                    } else {
                        MeshLogger.d(TAG, "[TRANSPORT-A]   Transport = BLE")
                    }

                    if (nextHop != null) {
                        MeshLogger.d(TAG, "[TRANSPORT-A]   ▶ Calling gattManager.broadcastPacket(includeAddress='$nextHop') -- DIRECTED")
                        gattManager.broadcastPacket(json, includeAddress = nextHop)
                    } else {
                        MeshLogger.d(TAG, "[TRANSPORT-A]   ▶ Calling gattManager.broadcastPacket() -- BROADCAST to ALL (${connectedNodes.size} nodes)")
                        gattManager.broadcastPacket(json)
                    }
                    // ────────────────────────────────────────────────────────────────────
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "[TRANSPORT-A]   ✗ EXCEPTION sending packet: ${e.message}")
                    MeshLogger.e(TAG, "Failed to send packet: ${e.message}")
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