package com.meshlink.ble.discovery

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.BleDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The central orchestrator for the Intelligent Discovery Engine.
 */
@Singleton
class DiscoveryEngine @Inject constructor(
    private val batteryAwareScanner: BatteryAwareScanner
) {
    private val TAG = "DiscoveryEngine"

    companion object {
        /** A peer with no discovery evidence for this long is removed from Nearby Devices. */
        const val PEER_STALE_TIMEOUT_MS = 60_000L
    }
    
    val analytics = DiscoveryAnalytics()
    val cache = DiscoveryCache()
    val connectionPolicy = SmartConnectionPolicy()
    private val duplicateFilter = DuplicateFilter()
    private val scheduler = DiscoveryScheduler(batteryAwareScanner)
    
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var scanJob: Job? = null
    
    // Callbacks to BleScannerManager
    var startScanAction: (() -> Unit)? = null
    var stopScanAction: (() -> Unit)? = null

    /**
     * Reports whether a GATT link to the given BLE address really exists. Installed by
     * [com.meshlink.ble.data.BleConnectionManager]. When null the recorded link flag is trusted.
     */
    @Volatile
    var linkProbe: ((String) -> Boolean)? = null
    
    private val _engineEvents = MutableSharedFlow<PeerDiscoveryRecord>(extraBufferCapacity = 50)
    val engineEvents: SharedFlow<PeerDiscoveryRecord> = _engineEvents.asSharedFlow()
    
    private val _scannedDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    val scannedDevices: StateFlow<Map<String, BleDevice>> = _scannedDevices.asStateFlow()

    val isScanning: Boolean
        get() = scanJob?.isActive == true

    private fun publishCache() {
        // Several records can map to one peer (BLE addresses rotate, so the same meshId shows up
        // under an old and a new MAC for a while). Publish the freshest record per key instead of
        // whichever happens to be iterated last, otherwise a stale record can shadow the live one.
        val map = cache.getAll()
            .groupBy { it.meshId.ifBlank { it.macAddress } }
            .mapValues { (_, records) ->
                val record = records.maxByOrNull { it.lastSeenMillis }!!
                val resolvedName = record.displayName?.takeIf { it.isNotBlank() } ?: "Mesh Peer"
                BleDevice(
                    meshId = record.meshId,
                    name = resolvedName,
                    address = record.macAddress,
                    rssi = record.smoothedRssi,
                    lastSeen = record.lastSeenMillis,
                    capabilities = record.capabilities,
                    isConnected = records.any { it.state == PeerLifecycleState.CONNECTED },
                    distanceMeters = record.distanceMeters,
                    distanceConfidence = record.distanceConfidence,
                    displayName = record.displayName,
                    bluetoothDeviceName = record.bluetoothDeviceName
                )
            }
        _scannedDevices.value = map
        MeshLogger.d(TAG, "[NearbyDiscovery] Published cache: ${map.size} devices (${map.keys})")
    }

    /**
     * Removes peers that have produced no discovery evidence for [PEER_STALE_TIMEOUT_MS] and
     * publishes the change. Peers with a live GATT link are never evicted while the link exists.
     */
    internal fun evictStalePeers(now: Long = System.currentTimeMillis()): Int {
        reconcileLinkFlags(now)
        val evicted = cache.evictStaleRecords(PEER_STALE_TIMEOUT_MS, now)
        evicted.forEach { record ->
            MeshLogger.i(
                TAG,
                "[PEER_EXPIRED] peer=${shortId(record.meshId.ifBlank { record.macAddress })} transport=BLE " +
                    "lastSeenAgeMs=${now - record.lastSeenMillis} state=${record.state} timeoutMs=$PEER_STALE_TIMEOUT_MS"
            )
        }
        if (evicted.isNotEmpty()) publishCache()
        return evicted.size
    }

    /**
     * A record flagged as linked whose GATT link no longer exists (closed locally, so no
     * disconnect callback ever arrived) must not be pinned in the registry forever. Drop the flag
     * and start a fresh freshness window, exactly as for a reported disconnect.
     */
    private fun reconcileLinkFlags(now: Long) {
        val probe = linkProbe ?: return
        cache.getAll().forEach { record ->
            if (record.linkActive && !probe(record.macAddress)) {
                record.linkActive = false
                record.lastSeenMillis = now
                MeshLogger.i(
                    TAG,
                    "[PEER_REGISTRY_REFRESH] peer=${shortId(record.meshId.ifBlank { record.macAddress })} transport=BLE reason=link_missing"
                )
            }
        }
    }

    private fun shortId(id: String): String =
        com.meshlink.util.MeshIdNormalizer.canonicalize(id).take(4).ifEmpty { "?" } + "…"

    fun start() {
        if (scanJob?.isActive == true) return
        
        MeshLogger.d(TAG, "[NearbyDiscovery] Starting Discovery Engine")
        
        scanJob = engineScope.launch {
            while (isActive) {
                // Determine window sizing based on current network state
                val hasConnections = analytics.metrics.value.activeConnections > 0
                val window = scheduler.getNextWindowConfig(hasConnections)
                
                MeshLogger.d(TAG, "[NearbyDiscovery] Scan Window: ${window.scanDurationMs}ms, Idle: ${window.idleDurationMs}ms")
                
                // Active Scan Phase
                try {
                    startScanAction?.invoke()
                    analytics.recordScanCycle()
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "[NearbyDiscovery] Failed to start hardware scan: ${e.message}")
                }
                
                delay(window.scanDurationMs)
                
                // Idle Phase
                try {
                    stopScanAction?.invoke()
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "[NearbyDiscovery] Failed to stop hardware scan: ${e.message}")
                }
                
                // Cleanup stale peers and duplicate filters during idle (60s configurable timeout to prevent Nearby flickering)
                evictStalePeers()
                duplicateFilter.prune()
                
                delay(window.idleDurationMs)
            }
        }
    }

    fun stop() {
        MeshLogger.d(TAG, "[NearbyDiscovery] Stopping Discovery Engine")
        scanJob?.cancel()
        scanJob = null
        try { stopScanAction?.invoke() } catch (_: Exception) {}
        duplicateFilter.clear()
    }

    /**
     * Entry point for raw BLE advertisements from the hardware scanner.
     */
    fun onDeviceDiscovered(
        macAddress: String,
        meshId: String,
        name: String,
        rssi: Int,
        capabilities: Byte = 0
    ) {
        if (!duplicateFilter.shouldProcess(macAddress)) {
            analytics.recordDuplicateSuppressed()
            // Suppression only skips the expensive processing. A valid advertisement is still
            // evidence that the peer is present, so its freshness must be refreshed; otherwise a
            // suppressed advertisement could let a live peer age out.
            cache.get(macAddress)?.lastSeenMillis = System.currentTimeMillis()
            return
        }
        
        analytics.recordPeerDiscovered()
        analytics.recordRssi(rssi)
        
        val isNewPeer = cache.get(macAddress) == null
        val record = cache.getOrPut(macAddress, meshId, name)
        if (isNewPeer) {
            MeshLogger.i(
                TAG,
                "[PEER_REGISTRY_REFRESH] peer=${shortId(meshId.ifBlank { macAddress })} transport=BLE accepted=new"
            )
        }
        if (meshId != macAddress && meshId.isNotBlank()) {
            record.meshId = meshId
        }
        
        // If the peer was suspended but we are receiving new advertisements, resume it
        if (connectionPolicy.getRetryState(macAddress) == com.meshlink.ble.discovery.RetryState.SUSPENDED) {
            connectionPolicy.resetPeer(macAddress)
        }
        
        // Store Bluetooth hardware device name purely as transport metadata
        if (name.isNotBlank()) {
            record.bluetoothDeviceName = name
        }
        // Do NOT overwrite record.name with the Bluetooth hardware phone model!
        if (record.displayName.isNullOrBlank()) {
            record.name = "Mesh Peer"
        }
        record.capabilities = capabilities
        record.lastSeenMillis = System.currentTimeMillis()
        
        // Smooth RSSI
        record.smoothedRssi = record.rssiFilter.filter(rssi.toDouble()).toInt()
        
        // Estimate Distance
        val estimate = DistanceEstimator.estimateDistance(
            rssi = record.smoothedRssi.toDouble(),
            errorCovariance = record.rssiFilter.getVariance()
        )
        record.distanceMeters = estimate.distanceMeters
        record.distanceConfidence = estimate.confidence.name
        
        // Calculate dynamic score
        record.score = PeerScoreCalculator.calculateScore(
            smoothedRssi = record.smoothedRssi,
            failedAttempts = record.failedAttempts,
            lastSeenMillis = record.lastSeenMillis
        )
        
        // Lifecycle transition
        if (record.state == PeerLifecycleState.UNKNOWN || record.state == PeerLifecycleState.LOST) {
            PeerLifecycleManager.transition(record, PeerLifecycleState.DISCOVERED)
        }
        
        // Notify downstream (Repository) that a peer was processed and scored
        _engineEvents.tryEmit(record)
        publishCache()
    }

    /**
     * Updates peer profile metadata (Mesh-Link display name and canonical mesh ID)
     * when received securely via handshake, beacon, or repository resolution.
     */
    fun updatePeerIdentity(identifier: String, displayName: String, canonicalMeshId: String? = null) {
        if (identifier.isBlank()) return
        val targetNorm = com.meshlink.util.MeshIdNormalizer.canonicalize(identifier)
        val record = cache.get(identifier)
            ?: cache.getAll().firstOrNull { 
                it.macAddress.equals(identifier, ignoreCase = true) ||
                it.meshId.equals(identifier, ignoreCase = true) ||
                com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) == targetNorm
            }

        if (record != null) {
            val trimmedName = displayName.trim()
            if (trimmedName.isNotBlank() && !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(trimmedName, canonicalMeshId ?: record.meshId)) {
                record.displayName = trimmedName
                record.name = trimmedName
            }
            if (!canonicalMeshId.isNullOrBlank() && canonicalMeshId.isNotBlank()) {
                record.meshId = canonicalMeshId
            }
            publishCache()
            MeshLogger.d(TAG, "[NearbyDiscovery] Updated peer identity: mac=${record.macAddress}, meshId=${record.meshId}, displayName='${record.displayName}'")
        }
    }

    fun notifyConnectionAttempt(macAddress: String) {
        cache.get(macAddress)?.let {
            PeerLifecycleManager.transition(it, PeerLifecycleState.CONNECTING)
        }
    }

    fun notifyConnectionSuccess(macAddress: String) {
        connectionPolicy.recordSuccess(macAddress)
        cache.get(macAddress)?.let {
            it.failedAttempts = 0
            it.linkActive = true
            PeerLifecycleManager.transition(it, PeerLifecycleState.CONNECTED)
        }
    }

    fun notifyConnectionFailure(macAddress: String, failureType: com.meshlink.ble.discovery.FailureType = com.meshlink.ble.discovery.FailureType.GATT_FAILURE) {
        connectionPolicy.recordFailure(macAddress, failureType)
        analytics.recordConnectionFailed()
        cache.get(macAddress)?.let {
            val wasLinked = it.linkActive ||
                it.state == PeerLifecycleState.CONNECTED ||
                it.state == PeerLifecycleState.AUTHENTICATING ||
                it.state == PeerLifecycleState.READY
            it.linkActive = false
            it.failedAttempts++
            PeerLifecycleManager.transition(it, PeerLifecycleState.DISCONNECTED)
            if (wasLinked) {
                // A live link is itself evidence the peer was present until now. While linked the
                // peer is not advertising, so lastSeen is frozen at link-up time; without this the
                // record would be considered stale (and evicted) the instant the link closes.
                // Start a fresh freshness window so the peer has time to be re-heard.
                it.lastSeenMillis = System.currentTimeMillis()
                MeshLogger.i(
                    TAG,
                    "[PEER_REGISTRY_REFRESH] peer=${shortId(it.meshId.ifBlank { it.macAddress })} transport=BLE reason=link_closed"
                )
            }
        }
    }
}
