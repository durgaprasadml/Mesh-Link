package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.BroadcastType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class HeartbeatPayload(
    val nodeId: String,
    val timestamp: Long,
    val batteryLevel: Int,
    val activeTransports: List<String>,
    val routeVersion: Long
)

@Singleton
class HeartbeatManager @Inject constructor(
    private val batteryAwareNetworking: BatteryAwareNetworking,
    private val congestionMonitor: CongestionMonitor,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "HeartbeatManager"
        private const val BASE_INTERVAL_MS = 15_000L // 15 seconds active base
        private const val IDLE_INTERVAL_MS = 45_000L // 45 seconds idle base
        private const val LOW_BATTERY_INTERVAL_MS = 120_000L // 2 minutes low battery
    }

    private var heartbeatJob: Job? = null
    private val routeVersion = AtomicLong(1L)
    private var lastEventSentTime = 0L

    // Tracks last seen timestamp per node ID
    val lastSeenPeers = ConcurrentHashMap<String, Long>()
    val peerHeartbeats = ConcurrentHashMap<String, HeartbeatPayload>()

    fun incrementRouteVersion() {
        routeVersion.incrementAndGet()
    }

    fun start(
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket) -> Unit
    ) {
        if (heartbeatJob?.isActive == true) return

        heartbeatJob = applicationScope.launch {
            while (isActive) {
                val interval = calculateAdaptiveInterval()
                delay(interval)

                // Skip periodic heartbeat if we sent an event-driven update recently (< interval/2)
                val now = System.currentTimeMillis()
                if (now - lastEventSentTime < interval / 2) {
                    continue
                }

                broadcastHeartbeat(localMeshId, sendPacketAction)
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Triggers an immediate event-driven heartbeat update (e.g. on route change, transport toggle, or state shift).
     */
    fun triggerEventDrivenHeartbeat(
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket) -> Unit
    ) {
        applicationScope.launch {
            MeshLogger.d(TAG, "Event-driven heartbeat triggered for $localMeshId")
            broadcastHeartbeat(localMeshId, sendPacketAction)
        }
    }

    private suspend fun broadcastHeartbeat(
        localMeshId: String,
        sendPacketAction: suspend (MeshPacket) -> Unit
    ) {
        if (localMeshId.isBlank()) return

        // Suppress heartbeat if congested to avoid flooding network
        if (congestionMonitor.congestionLevel.value == CongestionLevel.CRITICAL) {
            MeshLogger.d(TAG, "Heartbeat suppressed due to CRITICAL congestion.")
            return
        }

        val payloadObj = JSONObject().apply {
            put("nodeId", localMeshId)
            put("timestamp", System.currentTimeMillis())
            put("batteryLevel", batteryAwareNetworking.batteryPct.value)
            put("activeTransports", listOf("BLE", "WIFI_DIRECT"))
            put("routeVersion", routeVersion.get())
        }

        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = localMeshId,
            targetId = "BROADCAST",
            payload = payloadObj.toString(),
            type = PacketType.HEARTBEAT,
            priority = PacketPriority.LOW,
            broadcastType = BroadcastType.LOCAL,
            ttl = 1, // 1-hop neighborhood advertisement to avoid network clutter
            hopCount = 0,
            visitedPath = listOf(localMeshId)
        )

        try {
            sendPacketAction(packet)
            lastEventSentTime = System.currentTimeMillis()
            MeshLogger.d(TAG, "Sent heartbeat frame v${routeVersion.get()}")
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to send heartbeat: ${e.message}")
        }
    }

    /**
     * Processes an incoming heartbeat packet from a peer.
     */
    fun processHeartbeat(packet: MeshPacket): HeartbeatPayload? {
        return try {
            val json = JSONObject(packet.payload)
            val payload = HeartbeatPayload(
                nodeId = json.getString("nodeId"),
                timestamp = json.getLong("timestamp"),
                batteryLevel = json.getInt("batteryLevel"),
                activeTransports = json.getJSONArray("activeTransports").let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                routeVersion = json.getLong("routeVersion")
            )

            lastSeenPeers[payload.nodeId] = System.currentTimeMillis()
            peerHeartbeats[payload.nodeId] = payload
            payload
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error parsing heartbeat: ${e.message}")
            null
        }
    }

    private fun calculateAdaptiveInterval(): Long {
        if (batteryAwareNetworking.powerState.value == PowerState.CRITICAL) {
            return LOW_BATTERY_INTERVAL_MS
        }
        return if (congestionMonitor.isCongested()) {
            IDLE_INTERVAL_MS
        } else {
            BASE_INTERVAL_MS
        }
    }
}
