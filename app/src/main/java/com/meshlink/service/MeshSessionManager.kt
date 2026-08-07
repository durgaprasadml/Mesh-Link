package com.meshlink.service

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class PersistedNeighborSession(
    val meshId: String,
    val address: String,
    val lastRssi: Int,
    val lastSeenMillis: Long,
    val capabilities: Byte
)

@Singleton
class MeshSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MeshSessionManager"
        private const val PREFS_NAME = "mesh_session_store"
        private const val KEY_NEIGHBORS = "persisted_neighbors"
        private const val KEY_UPTIME_START = "session_start_time"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _cachedNeighbors = MutableStateFlow<Map<String, PersistedNeighborSession>>(emptyMap())
    val cachedNeighbors: StateFlow<Map<String, PersistedNeighborSession>> = _cachedNeighbors.asStateFlow()

    init {
        restoreSession()
    }

    fun saveNeighbor(meshId: String, address: String, rssi: Int, capabilities: Byte) {
        val current = _cachedNeighbors.value.toMutableMap()
        current[meshId] = PersistedNeighborSession(
            meshId = meshId,
            address = address,
            lastRssi = rssi,
            lastSeenMillis = System.currentTimeMillis(),
            capabilities = capabilities
        )
        _cachedNeighbors.value = current
        persistNeighborsToDisk(current.values.toList())
    }

    fun restoreSession(): List<PersistedNeighborSession> {
        val jsonStr = prefs.getString(KEY_NEIGHBORS, null) ?: return emptyList()
        val list = mutableListOf<PersistedNeighborSession>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val meshId = obj.getString("meshId")
                val address = obj.getString("address")
                val rssi = obj.getInt("lastRssi")
                val lastSeen = obj.getLong("lastSeenMillis")
                val caps = obj.getInt("capabilities").toByte()
                list.add(PersistedNeighborSession(meshId, address, rssi, lastSeen, caps))
            }
            _cachedNeighbors.value = list.associateBy { it.meshId }
            MeshLogger.d(TAG, "Restored ${list.size} persisted mesh neighbors from session storage")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to restore session storage: ${e.message}")
        }
        return list
    }

    fun clearSession() {
        _cachedNeighbors.value = emptyMap()
        prefs.edit().remove(KEY_NEIGHBORS).apply()
    }

    private fun persistNeighborsToDisk(neighbors: List<PersistedNeighborSession>) {
        try {
            val array = JSONArray()
            neighbors.forEach { n ->
                val obj = JSONObject().apply {
                    put("meshId", n.meshId)
                    put("address", n.address)
                    put("lastRssi", n.lastRssi)
                    put("lastSeenMillis", n.lastSeenMillis)
                    put("capabilities", n.capabilities.toInt())
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_NEIGHBORS, array.toString()).apply()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error persisting neighbors to disk: ${e.message}")
        }
    }
}
