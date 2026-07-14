package com.meshlink.analytics.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.meshlink.common.logger.MeshLogger

/**
 * Collects mesh network analytics — packet delivery rates, hop counts,
 * relay activity, and failure logs. Exposed as reactive flows for the dashboard.
 */
@Singleton
class MeshAnalytics @Inject constructor() {

    // ────────── Counters ──────────
    private var totalPacketsSent = 0
    private var totalPacketsDelivered = 0
    private var totalPacketsFailed = 0
    private var totalPacketsRelayed = 0
    private var totalHops = 0L
    private var hopSamples = 0

    // ────────── Live State Flows ──────────

    private val _stats = MutableStateFlow(MeshStats())
    val stats: StateFlow<MeshStats> = _stats.asStateFlow()

    private val _recentRelayLog = MutableStateFlow<List<RelayLogEntry>>(emptyList())
    val recentRelayLog: StateFlow<List<RelayLogEntry>> = _recentRelayLog.asStateFlow()

    private val _activeNodes = MutableStateFlow<Set<String>>(emptySet())
    val activeNodes: StateFlow<Set<String>> = _activeNodes.asStateFlow()

    private val _hopDistribution = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val hopDistribution: StateFlow<Map<Int, Int>> = _hopDistribution.asStateFlow()

    // ────────── Recording Methods ──────────

    fun recordPacketSent() {
        totalPacketsSent++
        refreshStats()
    }

    fun recordPacketDelivered(hopCount: Int) {
        totalPacketsDelivered++
        totalHops += hopCount
        hopSamples++

        // Update hop distribution
        val dist = _hopDistribution.value.toMutableMap()
        dist[hopCount] = (dist[hopCount] ?: 0) + 1
        _hopDistribution.value = dist

        refreshStats()
    }

    fun recordPacketFailed(targetId: String, reason: String) {
        totalPacketsFailed++
        addLogEntry(RelayLogEntry(
            timestamp = System.currentTimeMillis(),
            event = "❌ FAILED → ${targetId.takeLast(8)}",
            detail = reason,
            type = LogType.FAILURE
        ))
        refreshStats()
    }

    fun recordPacketRelayed(fromId: String, hopCount: Int) {
        totalPacketsRelayed++
        addLogEntry(RelayLogEntry(
            timestamp = System.currentTimeMillis(),
            event = "🔄 RELAYED from ${fromId.takeLast(8)}",
            detail = "Hop #$hopCount",
            type = LogType.RELAY
        ))
        refreshStats()
    }

    fun recordNodeSeen(nodeId: String) {
        val nodes = _activeNodes.value.toMutableSet()
        nodes.add(nodeId)
        _activeNodes.value = nodes
    }

    fun recordNodeLost(nodeId: String) {
        val nodes = _activeNodes.value.toMutableSet()
        nodes.remove(nodeId)
        _activeNodes.value = nodes
    }

    fun recordKeyExchange(peerId: String) {
        addLogEntry(RelayLogEntry(
            timestamp = System.currentTimeMillis(),
            event = "🔐 KEY_EXCHANGE with ${peerId.takeLast(8)}",
            detail = "E2E encryption established",
            type = LogType.SECURITY
        ))
    }

    fun recordSosReceived(senderId: String) {
        addLogEntry(RelayLogEntry(
            timestamp = System.currentTimeMillis(),
            event = "🚨 SOS from ${senderId.takeLast(8)}",
            detail = "Emergency broadcast received",
            type = LogType.SOS
        ))
    }

    // ────────── Internal ──────────

    private fun refreshStats() {
        _stats.value = MeshStats(
            packetsSent = totalPacketsSent,
            packetsDelivered = totalPacketsDelivered,
            packetsFailed = totalPacketsFailed,
            packetsRelayed = totalPacketsRelayed,
            deliveryRate = if (totalPacketsSent > 0) {
                (totalPacketsDelivered * 100f / totalPacketsSent)
            } else 0f,
            avgHopCount = if (hopSamples > 0) {
                totalHops.toFloat() / hopSamples
            } else 0f,
            activeNodeCount = _activeNodes.value.size
        )
    }

    private fun addLogEntry(entry: RelayLogEntry) {
        val log = _recentRelayLog.value.toMutableList()
        log.add(0, entry) // Newest first
        if (log.size > 100) log.removeLast()
        _recentRelayLog.value = log
    }
}

data class MeshStats(
    val packetsSent: Int = 0,
    val packetsDelivered: Int = 0,
    val packetsFailed: Int = 0,
    val packetsRelayed: Int = 0,
    val deliveryRate: Float = 0f,
    val avgHopCount: Float = 0f,
    val activeNodeCount: Int = 0,
    val averageLatencyMs: Long = 0L,
    val networkDiameter: Int = 0,
    val congestionScore: Int = 0,
    val healthScore: Int = 100
)

data class RelayLogEntry(
    val timestamp: Long,
    val event: String,
    val detail: String,
    val type: LogType
)

enum class LogType {
    RELAY, FAILURE, SECURITY, SOS
}
