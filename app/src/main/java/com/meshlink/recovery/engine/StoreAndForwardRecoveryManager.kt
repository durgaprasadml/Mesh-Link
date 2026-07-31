package com.meshlink.recovery.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.RelayPacketEntity
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import com.meshlink.routing.engine.DuplicateSuppressionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreAndForwardRecoveryManager @Inject constructor(
    private val relayDao: RelayDao,
    private val duplicateSuppressionEngine: DuplicateSuppressionEngine,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "StoreAndForwardRecovery"
        private const val DEFAULT_TTL_MS = 86_400_000L // 24 hours
        private const val STORAGE_CAP = 1000 // Max 1000 store & forward packets
    }

    private val _queuedCount = MutableStateFlow(0)
    val queuedCount: StateFlow<Int> = _queuedCount.asStateFlow()

    init {
        applicationScope.launch {
            refreshCount()
            cleanupExpired()
        }
    }

    suspend fun enqueue(packet: MeshPacket) {
        val entity = RelayPacketEntity(
            packetId = packet.packetId,
            senderId = packet.senderId,
            targetId = packet.targetId,
            payload = packet.payload,
            type = packet.type.name,
            priority = packet.priority.name,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + DEFAULT_TTL_MS,
            ttl = packet.ttl,
            hopCount = packet.hopCount,
            encrypted = packet.encrypted
        )

        relayDao.insertPacket(entity)
        relayDao.enforceStorageCap(STORAGE_CAP)
        refreshCount()
        MeshLogger.d(TAG, "Enqueued packet ${packet.packetId} (target=${packet.targetId}, priority=${packet.priority}) for Store-and-Forward")
    }

    /**
     * Called when a peer connects or reconnects. Flushes all pending store-and-forward packets for that target.
     */
    fun flushPendingForPeer(
        peerId: String,
        sendPacketAction: suspend (MeshPacket) -> Unit
    ) {
        applicationScope.launch {
            val pendingEntities = relayDao.getPacketsForTarget(peerId)
            if (pendingEntities.isEmpty()) return@launch

            MeshLogger.i(TAG, "Flushing ${pendingEntities.size} store-and-forward packets for reconnected peer $peerId...")

            // Priority-aware sorting: CRITICAL > HIGH > NORMAL > LOW > BACKGROUND
            val sorted = pendingEntities.sortedByDescending { entity ->
                try {
                    PacketPriority.valueOf(entity.priority).level
                } catch (e: Exception) {
                    1
                }
            }

            for (entity in sorted) {
                val packet = MeshPacket(
                    packetId = entity.packetId,
                    senderId = entity.senderId,
                    targetId = entity.targetId,
                    payload = entity.payload,
                    type = try { PacketType.valueOf(entity.type) } catch (e: Exception) { PacketType.TEXT },
                    priority = try { PacketPriority.valueOf(entity.priority) } catch (e: Exception) { PacketPriority.NORMAL },
                    ttl = entity.ttl,
                    hopCount = entity.hopCount
                )

                // Avoid duplicate delivery
                if (!duplicateSuppressionEngine.checkAndMark(packet)) {
                    try {
                        sendPacketAction(packet)
                        relayDao.deletePacket(entity.packetId)
                        MeshLogger.d(TAG, "Store-and-forward packet ${entity.packetId} delivered to $peerId")
                    } catch (e: Exception) {
                        MeshLogger.w(TAG, "Failed to flush packet ${entity.packetId} to $peerId: ${e.message}")
                    }
                } else {
                    // Was duplicate, remove from queue
                    relayDao.deletePacket(entity.packetId)
                }
            }
            refreshCount()
        }
    }

    suspend fun getAllPendingIds(): List<String> {
        return relayDao.getAllRelayPackets().map { it.packetId }
    }

    suspend fun cleanupExpired() {
        relayDao.deleteExpiredPackets(System.currentTimeMillis())
        refreshCount()
    }

    private suspend fun refreshCount() {
        val all = relayDao.getAllRelayPackets()
        _queuedCount.value = all.size
    }
}
