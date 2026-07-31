package com.meshlink.recovery.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.BroadcastType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ManifestSummary(
    val senderId: String,
    val routeVersion: Long,
    val pendingPacketIds: List<String>,
    val manifestHash: String
)

@Singleton
class PartitionRecoveryManager @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "PartitionRecoveryManager"
    }

    private val _partitionEventCount = MutableStateFlow(0)
    val partitionEventCount: StateFlow<Int> = _partitionEventCount.asStateFlow()

    private val _healEventCount = MutableStateFlow(0)
    val healEventCount: StateFlow<Int> = _healEventCount.asStateFlow()

    // Key: Peer ID -> last known manifest hash to prevent duplicate syncs
    private val peerManifestHashes = ConcurrentHashMap<String, String>()

    fun recordPartitionSplit() {
        _partitionEventCount.value += 1
        MeshLogger.w(TAG, "Network partition (split) detected!")
    }

    /**
     * Called when a peer from another mesh cluster segment reconnects or sends a manifest.
     */
    fun handlePeerReconnection(
        peerId: String,
        localMeshId: String,
        localPendingPacketIds: List<String>,
        localRouteVersion: Long,
        sendPacketAction: suspend (MeshPacket, String) -> Unit
    ) {
        applicationScope.launch {
            _healEventCount.value += 1
            MeshLogger.i(TAG, "Partition reconnect detected with peer $peerId. Sending compact anti-entropy manifest...")

            // Send compact manifest
            val manifestHash = generateHash(localRouteVersion, localPendingPacketIds)
            val manifestObj = JSONObject().apply {
                put("senderId", localMeshId)
                put("routeVersion", localRouteVersion)
                put("pendingPacketIds", JSONArray(localPendingPacketIds))
                put("manifestHash", manifestHash)
            }

            val packet = MeshPacket(
                packetId = UUID.randomUUID().toString(),
                senderId = localMeshId,
                targetId = peerId,
                payload = manifestObj.toString(),
                type = PacketType.PARTITION_SYNC_MANIFEST,
                priority = PacketPriority.HIGH,
                ttl = 3,
                visitedPath = listOf(localMeshId)
            )

            sendPacketAction(packet, peerId)
        }
    }

    /**
     * Processes an incoming compact PARTITION_SYNC_MANIFEST from a peer.
     * Evaluates missing item IDs and responds with a PARTITION_SYNC_REQUEST if needed.
     */
    fun processSyncManifest(
        packet: MeshPacket,
        localMeshId: String,
        localPendingIds: Set<String>,
        sendPacketAction: suspend (MeshPacket, String) -> Unit
    ): List<String> {
        return try {
            val json = JSONObject(packet.payload)
            val senderId = json.getString("senderId")
            val manifestHash = json.getString("manifestHash")

            // Avoid duplicate synchronization if manifest hasn't changed
            if (peerManifestHashes[senderId] == manifestHash) {
                MeshLogger.d(TAG, "Manifest from $senderId unchanged ($manifestHash). Skipping duplicate sync.")
                return emptyList()
            }
            peerManifestHashes[senderId] = manifestHash

            val remotePacketIds = json.getJSONArray("pendingPacketIds").let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }

            // Find missing IDs that remote peer has but local node lacks
            val missingFromLocal = remotePacketIds.filter { !localPendingIds.contains(it) }

            if (missingFromLocal.isNotEmpty()) {
                MeshLogger.i(TAG, "Incremental Sync: Requesting ${missingFromLocal.size} missing items from $senderId")

                val requestObj = JSONObject().apply {
                    put("requesterId", localMeshId)
                    put("requestedPacketIds", JSONArray(missingFromLocal))
                }

                val reqPacket = MeshPacket(
                    packetId = UUID.randomUUID().toString(),
                    senderId = localMeshId,
                    targetId = senderId,
                    payload = requestObj.toString(),
                    type = PacketType.PARTITION_SYNC_REQUEST,
                    priority = PacketPriority.HIGH,
                    ttl = 3,
                    visitedPath = listOf(localMeshId)
                )

                applicationScope.launch {
                    sendPacketAction(reqPacket, senderId)
                }
            } else {
                MeshLogger.d(TAG, "Incremental Sync: Both partition segments are already in sync with peer $senderId")
            }

            missingFromLocal
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error parsing PARTITION_SYNC_MANIFEST: ${e.message}")
            emptyList()
        }
    }

    private fun generateHash(routeVersion: Long, ids: List<String>): String {
        val combined = "$routeVersion:${ids.sorted().joinToString(",")}"
        return combined.hashCode().toString(16)
    }
}
