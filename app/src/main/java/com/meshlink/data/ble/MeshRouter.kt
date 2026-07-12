package com.meshlink.data.ble

import android.util.Log
import com.meshlink.data.analytics.MeshAnalytics
import com.meshlink.data.local.RelayDao
import com.meshlink.data.local.RelayPacketEntity
import com.meshlink.data.wifi.WifiDirectManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Collections
import java.util.LinkedHashSet
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshRouter @Inject constructor(
    private val gattManager: BleGattManager,
    private val analytics: MeshAnalytics,
    private val relayDao: RelayDao
) {

    companion object {
        private const val TAG = "MeshRouter"
        private const val RECONNECT_INTERVAL_MS = 10_000L
        private const val DEDUP_CACHE_SIZE = 2000
    }

    var localMeshId: String = ""

    data class RouteEntry(
        val nextHop: String,
        val hops: Int,
        val lastSeen: Long
    )

    val routeTable = ConcurrentHashMap<String, RouteEntry>()

    private val processedPackets: MutableSet<String> = Collections.synchronizedSet(
        object : LinkedHashSet<String>() {
            override fun add(element: String): Boolean {
                if (size >= DEDUP_CACHE_SIZE) {
                    val first = iterator().next()
                    remove(first)
                }
                return super.add(element)
            }
        }
    )

    private val _incomingPayloads =
        MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 200)

    val incomingPayloads: SharedFlow<Pair<String, MeshPacket>> =
        _incomingPayloads.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeIncoming()
        startReconnectLoop()
        startRouteCleanupLoop()
    }

    // ─────────────────── Incoming Observation ───────────────────

    private fun observeIncoming() {
        scope.launch {
            gattManager.incomingMessages.collect { (sender, json) ->
                try {
                    handleIncomingPacket(sender, json)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling BLE packet from $sender: ${e.message}")
                }
            }
        }
    }

    // ─────────────────── Store-and-Forward Loop ───────────────────
    // ONLY for packets that could NOT be forwarded immediately (no peers at the time)

    private fun startStoreAndForwardLoop() {
        scope.launch {
            while (isActive) {
                delay(30_000L)
                try {
                    tryDeliverCachedPackets()
                    relayDao.deleteExpiredPackets(System.currentTimeMillis())
                } catch (e: Exception) {
                    Log.e(TAG, "Store-and-forward loop error: ${e.message}")
                }
            }
        }
    }

    private suspend fun tryDeliverCachedPackets() {
        val cachedPackets = relayDao.getAllRelayPackets()
        if (cachedPackets.isEmpty()) return

        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
        if (connectedNodes.isEmpty()) {
            Log.d(TAG, "S&F: no connected peers, keeping ${cachedPackets.size} cached packets")
            return
        }

        Log.d(TAG, "S&F: attempting delivery of ${cachedPackets.size} cached packets to ${connectedNodes.size} peer(s)")

        cachedPackets.forEach { entity ->
            if (entity.ttl <= 0) {
                relayDao.deletePacket(entity.packetId)
                return@forEach
            }

            val packet = MeshPacket(
                packetId = entity.packetId,
                senderId = entity.senderId,
                targetId = entity.targetId,
                payload = entity.payload,
                type = try { PacketType.valueOf(entity.type) } catch (_: Exception) { PacketType.TEXT },
                transferId = entity.transferId,
                chunkIndex = entity.chunkIndex,
                totalChunks = entity.totalChunks,
                mimeType = entity.mimeType,
                encrypted = entity.encrypted,
                ttl = entity.ttl - 1,
                hopCount = entity.hopCount + 1
            )

            processedPackets.add(entity.packetId)

            val json = MeshPacketParser.toJson(packet)
            gattManager.broadcastPacket(json)
            relayDao.deletePacket(entity.packetId)
            Log.d(TAG, "S&F: delivered ${entity.packetId.takeLast(6)}")
        }
    }

    // ─────────────────── Reconnect Loop ───────────────────

    private fun startReconnectLoop() {
        scope.launch {
            while (isActive) {
                delay(RECONNECT_INTERVAL_MS)
                val knownAddresses = routeTable.values.map { it.nextHop }.filter { it.isNotBlank() }
                if (knownAddresses.isNotEmpty() && gattManager.activeClients.isEmpty() && gattManager.connectedServers.isEmpty()) {
                    Log.d(TAG, "Reconnect loop: retrying ${knownAddresses.size} known peer(s)")
                    knownAddresses.forEach { address ->
                        try {
                            gattManager.connectToDevice(address)
                        } catch (e: Exception) {
                            Log.w(TAG, "Reconnect failed for $address: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // ─────────────────── Route Cleanup Loop ───────────────────

    private fun startRouteCleanupLoop() {
        scope.launch {
            while (isActive) {
                delay(60_000L) // Check every minute
                val now = System.currentTimeMillis()
                val expiredThreshold = 15 * 60 * 1000L // 15 minutes
                routeTable.entries.removeIf { now - it.value.lastSeen > expiredThreshold }
            }
        }
    }

    // ─────────────────── Core Packet Handler ───────────────────

    private fun handleIncomingPacket(
        immediateSenderAddress: String,
        json: String
    ) {
        if (json.isBlank() || !json.trimStart().startsWith("{")) {
            Log.w(TAG, "Dropped malformed packet from $immediateSenderAddress")
            return
        }

        val packet = MeshPacketParser.fromJson(json)
        if (packet == null) {
            Log.w(TAG, "JSON parse failed from $immediateSenderAddress")
            return
        }

        // Strict de-dup — reject if already processed
        if (!processedPackets.add(packet.packetId)) {
            Log.d(TAG, "Dedup: dropped duplicate ${packet.packetId.takeLast(6)}")
            return
        }

        // Route learning - update if new route is shorter or existing route is older than 5 mins
        val now = System.currentTimeMillis()
        val existingRoute = routeTable[packet.senderId]
        if (existingRoute == null || 
            packet.hopCount <= existingRoute.hops || 
            (now - existingRoute.lastSeen) > 5 * 60 * 1000L) {
            routeTable[packet.senderId] = RouteEntry(
                nextHop = immediateSenderAddress,
                hops = packet.hopCount,
                lastSeen = now
            )
        }

        Log.d(TAG, "Packet [${packet.type}] from=${packet.senderId.takeLast(6)} target=${packet.targetId.takeLast(6)} ttl=${packet.ttl} hops=${packet.hopCount}")

        analytics.recordNodeSeen(packet.senderId)

        val isBroadcast = packet.targetId == "BROADCAST"
        val isForMe     = packet.targetId == localMeshId

        // Deliver locally if it's for us or a broadcast
        if (isForMe || isBroadcast) {
            analytics.recordPacketDelivered(packet.hopCount)
            val emitted = _incomingPayloads.tryEmit(packet.senderId to packet)
            if (!emitted) {
                Log.w(TAG, "incomingPayloads buffer full — packet ${packet.packetId.takeLast(6)} dropped")
            }
        }

        // Packets FOR US: do not forward or store (they reached their destination)
        if (isForMe) {
            Log.d(TAG, "Packet ${packet.packetId.takeLast(6)} delivered locally, not forwarding")
            return
        }

        // ACK/NACK are ephemeral — do not store-and-forward, just relay if needed
        val isAckNack = packet.type == PacketType.MEDIA_ACK || packet.type == PacketType.MEDIA_NACK

        // TTL check
        if (packet.ttl <= 0) {
            Log.d(TAG, "TTL exhausted for ${packet.packetId.takeLast(6)}, not forwarding")
            return
        }

        // Loop guard
        if (localMeshId.isNotBlank() && packet.visitedPath.contains(localMeshId)) {
            Log.d(TAG, "Loop guard: already visited ${packet.packetId.takeLast(6)}, skipping forward")
            return
        }

        val relayPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            visitedPath = (packet.visitedPath.toMutableList().also {
                if (localMeshId.isNotBlank()) it.add(localMeshId)
            })
        )

        analytics.recordPacketRelayed(packet.senderId, relayPacket.hopCount)

        val forwardedJson = MeshPacketParser.toJson(relayPacket)

        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
        val hasPeersToForward = connectedNodes.any { it != immediateSenderAddress }

        if (hasPeersToForward) {
            // Relay optimization: If we have a known route for the target (and it's not broadcast), 
            // try to send ONLY to the next hop instead of broadcasting to everyone.
            val targetRoute = routeTable[packet.targetId]
            if (targetRoute != null && connectedNodes.contains(targetRoute.nextHop) && targetRoute.nextHop != immediateSenderAddress) {
                // Send only to the specific next hop
                gattManager.broadcastPacket(forwardedJson, includeAddress = targetRoute.nextHop)
                Log.d(TAG, "Directed relay ${packet.packetId.takeLast(6)} via ${targetRoute.nextHop}")
            } else {
                // Broadcast to all except sender
                gattManager.broadcastPacket(forwardedJson, excludeAddress = immediateSenderAddress)
                Log.d(TAG, "Forwarded ${packet.packetId.takeLast(6)} immediately (ttl=${relayPacket.ttl})")
            }
        } else if (!isAckNack) {
            // No peers available — store for later (skip for ephemeral ACK/NACK)
            scope.launch {
                try {
                    relayDao.insertPacket(
                        RelayPacketEntity(
                            packetId    = packet.packetId,
                            senderId    = packet.senderId,
                            targetId    = packet.targetId,
                            payload     = packet.payload,
                            type        = packet.type.name,
                            ttl         = packet.ttl,
                            hopCount    = packet.hopCount,
                            encrypted   = packet.encrypted,
                            transferId  = packet.transferId,
                            chunkIndex  = packet.chunkIndex,
                            totalChunks = packet.totalChunks,
                            mimeType    = packet.mimeType
                        )
                    )
                    Log.d(TAG, "Stored ${packet.packetId.takeLast(6)} for later delivery (no peers)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache relay packet: ${e.message}")
                }
            }
        }
    }

    // ─────────────────── Send Methods ───────────────────

    /**
     * FIX ISSUE 1 & 2: Accept explicit packetId so retries use the same ID.
     * This prevents duplicate messages when retryPendingMessages re-sends.
     */
    fun sendPayload(
        targetId: String,
        payload: String,
        myAddressAlias: String = "Me",
        encrypted: Boolean = false,
        packetId: String? = null
    ) {
        val packet = MeshPacket(
            packetId = packetId ?: java.util.UUID.randomUUID().toString(),
            senderId = myAddressAlias,
            targetId = targetId,
            payload = payload,
            encrypted = encrypted,
            ttl = 10
        )

        // Register own packet to prevent re-processing if it bounces back
        processedPackets.add(packet.packetId)
        analytics.recordPacketSent()

        val json = MeshPacketParser.toJson(packet)
        Log.d(TAG, "sendPayload → $targetId (encrypted=$encrypted, packetId=${packet.packetId.takeLast(6)})")

        // Direct sending optimization
        val targetRoute = routeTable[targetId]
        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
        if (targetRoute != null && connectedNodes.contains(targetRoute.nextHop)) {
            gattManager.broadcastPacket(json, includeAddress = targetRoute.nextHop)
            Log.d(TAG, "Directed send via ${targetRoute.nextHop}")
        } else {
            gattManager.broadcastPacket(json)
        }
    }

    fun sendMediaPacket(packet: MeshPacket) {
        processedPackets.add(packet.packetId)
        analytics.recordPacketSent()

        val json = MeshPacketParser.toJson(packet)
        Log.d(TAG, "sendMediaPacket [${packet.type}] transferId=${packet.transferId?.takeLast(6)} chunk=${packet.chunkIndex}/${packet.totalChunks}")

        // Direct sending optimization for media
        val targetRoute = routeTable[packet.targetId]
        val connectedNodes = gattManager.connectedServers.keys + gattManager.activeClients.keys
        if (targetRoute != null && connectedNodes.contains(targetRoute.nextHop)) {
            gattManager.broadcastPacket(json, includeAddress = targetRoute.nextHop)
        } else {
            gattManager.broadcastPacket(json)
        }
    }

    fun broadcastKeyExchange(
        myMeshId: String,
        publicKeyBase64: String
    ) {
        val packet = MeshPacket(
            senderId = myMeshId,
            targetId = "BROADCAST",
            payload = publicKeyBase64,
            type = PacketType.KEY_EXCHANGE,
            ttl = 10
        )

        sendMediaPacket(packet)
        Log.d(TAG, "KEY_EXCHANGE broadcast from ${myMeshId.takeLast(6)}")
    }
}