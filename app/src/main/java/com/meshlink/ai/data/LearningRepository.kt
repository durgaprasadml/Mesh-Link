package com.meshlink.ai.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data structure to hold historical metrics for a specific peer.
 */
data class PeerMetrics(
    var totalPacketsSent: Long = 0,
    var totalPacketsDelivered: Long = 0,
    var totalBytesTransferred: Long = 0,
    var averageLatencyMs: Long = 0,
    var successfulBleConnections: Long = 0,
    var successfulWifiConnections: Long = 0,
    var failedConnections: Long = 0,
    var lastSeenTimestamp: Long = 0,
    var connectionDrops: Long = 0
)

@Singleton
class LearningRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mesh_ai_learning", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // In-memory cache for ultra-fast prediction access
    private val peerMetricsMap = ConcurrentHashMap<String, PeerMetrics>()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val json = prefs.getString("peer_metrics", "{}")
        val type = object : TypeToken<Map<String, PeerMetrics>>() {}.type
        val map: Map<String, PeerMetrics> = gson.fromJson(json, type) ?: emptyMap()
        peerMetricsMap.putAll(map)
    }

    fun saveToDisk() {
        ioScope.launch {
            val json = gson.toJson(peerMetricsMap)
            prefs.edit().putString("peer_metrics", json).apply()
        }
    }

    fun getMetricsForPeer(peerId: String): PeerMetrics {
        return peerMetricsMap.getOrPut(peerId) { PeerMetrics() }
    }

    fun updateMetrics(peerId: String, updateBlock: (PeerMetrics) -> Unit) {
        val metrics = getMetricsForPeer(peerId)
        updateBlock(metrics)
        // Periodically save to disk, typically triggered by a background timer 
        // to avoid constant I/O, but we can trigger it here for simplicity
        if (Math.random() < 0.05) { // 5% chance to flush to disk on update
            saveToDisk()
        }
    }
    
    // Global metrics (e.g. baseline congestion, battery cost)
    fun getGlobalMetric(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun setGlobalMetric(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }
}
