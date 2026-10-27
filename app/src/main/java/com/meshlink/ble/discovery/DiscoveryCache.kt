package com.meshlink.ble.discovery

import java.util.concurrent.ConcurrentHashMap

data class PeerDiscoveryRecord(
    val macAddress: String,
    var meshId: String,
    var name: String,
    val rssiFilter: KalmanFilter = KalmanFilter(),
    var smoothedRssi: Int = -100,
    var distanceMeters: Double? = null,
    var distanceConfidence: String? = null,
    var lastSeenMillis: Long = System.currentTimeMillis(),
    var failedAttempts: Int = 0,
    var capabilities: Byte = 0, // Battery, Routing, etc.
    var score: Int = 0,
    var state: PeerLifecycleState = PeerLifecycleState.UNKNOWN,
    var displayName: String? = null,
    var bluetoothDeviceName: String? = null,
    /**
     * True while a GATT link to this peer is up. Tracked separately from [state] because the
     * lifecycle matrix cannot reach CONNECTED from DISCOVERED. A linked peer is not advertising
     * (connectable advertisements are consumed by the link), so its [lastSeenMillis] is frozen and
     * must not be used to expire it.
     */
    @Volatile var linkActive: Boolean = false
)

/**
 * In-memory thread-safe cache of all discovered peers.
 */
class DiscoveryCache {
    private val peers = ConcurrentHashMap<String, PeerDiscoveryRecord>()

    private val MAX_PEERS = 1000

    fun getOrPut(macAddress: String, meshId: String, defaultName: String): PeerDiscoveryRecord {
        if (peers.size >= MAX_PEERS && !peers.containsKey(macAddress)) {
            // Evict oldest peer to stay within memory limits
            evictOldest()
        }
        return peers.getOrPut(macAddress) {
            PeerDiscoveryRecord(
                macAddress = macAddress,
                meshId = meshId,
                name = "Mesh Peer",
                bluetoothDeviceName = defaultName.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun evictOldest() {
        peers.values.minByOrNull { it.lastSeenMillis }?.let { oldest ->
            if (oldest.state != PeerLifecycleState.CONNECTED && oldest.state != PeerLifecycleState.CONNECTING && !oldest.linkActive) {
                peers.remove(oldest.macAddress)
            }
        }
    }

    fun get(macAddress: String): PeerDiscoveryRecord? {
        return peers[macAddress]
    }

    fun getAll(): List<PeerDiscoveryRecord> {
        return peers.values.toList()
    }

    /**
     * Removes peers that haven't been seen in the specified timeout.
     * @return Number of evicted peers.
     */
    fun evictStale(timeoutMillis: Long): Int = evictStaleRecords(timeoutMillis).size

    /**
     * Same policy as [evictStale] but returns the evicted records so the caller can log and
     * publish the change. [now] is injectable for deterministic tests.
     */
    fun evictStaleRecords(
        timeoutMillis: Long,
        now: Long = System.currentTimeMillis()
    ): List<PeerDiscoveryRecord> {
        val evicted = mutableListOf<PeerDiscoveryRecord>()
        val iterator = peers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val record = entry.value
            // Only evict if not connected/connecting
            if ((now - record.lastSeenMillis > timeoutMillis) && 
                record.state != PeerLifecycleState.CONNECTED &&
                record.state != PeerLifecycleState.CONNECTING &&
                record.state != PeerLifecycleState.READY &&
                !record.linkActive) {
                
                iterator.remove()
                evicted.add(record)
            }
        }
        return evicted
    }
    
    fun clear() {
        peers.clear()
    }
}
