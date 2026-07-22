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
    
    private val _engineEvents = MutableSharedFlow<PeerDiscoveryRecord>(extraBufferCapacity = 50)
    val engineEvents: SharedFlow<PeerDiscoveryRecord> = _engineEvents.asSharedFlow()
    
    private val _scannedDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    val scannedDevices: StateFlow<Map<String, BleDevice>> = _scannedDevices.asStateFlow()

    private fun publishCache() {
        val map = cache.getAll().associate { record ->
            record.meshId to BleDevice(
                meshId = record.meshId,
                name = record.name,
                address = record.macAddress,
                rssi = record.smoothedRssi,
                distanceMeters = record.distanceMeters,
                distanceConfidence = record.distanceConfidence
            )
        }
        _scannedDevices.value = map
    }

    fun start() {
        if (scanJob?.isActive == true) return
        
        MeshLogger.d(TAG, "Starting Intelligent Discovery Engine")
        
        scanJob = engineScope.launch {
            while (isActive) {
                // Determine window sizing based on current network state
                val hasConnections = analytics.metrics.value.activeConnections > 0
                val window = scheduler.getNextWindowConfig(hasConnections)
                
                MeshLogger.d(TAG, "Scan Window: ${window.scanDurationMs}ms, Idle: ${window.idleDurationMs}ms")
                
                // Active Scan Phase
                try {
                    startScanAction?.invoke()
                    analytics.recordScanCycle()
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to start hardware scan: ${e.message}")
                }
                
                delay(window.scanDurationMs)
                
                // Idle Phase
                try {
                    stopScanAction?.invoke()
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to stop hardware scan: ${e.message}")
                }
                
                // Cleanup stale peers and duplicate filters during idle
                val evicted = cache.evictStale(timeoutMillis = 30000L) // 30 seconds stale
                if (evicted > 0) publishCache()
                duplicateFilter.prune()
                
                delay(window.idleDurationMs)
            }
        }
    }

    fun stop() {
        MeshLogger.d(TAG, "Stopping Discovery Engine")
        scanJob?.cancel()
        scanJob = null
        engineScope.cancel()
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
            return
        }
        
        analytics.recordPeerDiscovered()
        analytics.recordRssi(rssi)
        
        val record = cache.getOrPut(macAddress, meshId, name)
        
        // Ensure name is updated if changed
        record.name = name
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

    fun notifyConnectionAttempt(macAddress: String) {
        cache.get(macAddress)?.let {
            PeerLifecycleManager.transition(it, PeerLifecycleState.CONNECTING)
        }
    }

    fun notifyConnectionSuccess(macAddress: String) {
        connectionPolicy.recordSuccess(macAddress)
        cache.get(macAddress)?.let {
            it.failedAttempts = 0
            PeerLifecycleManager.transition(it, PeerLifecycleState.CONNECTED)
        }
    }

    fun notifyConnectionFailure(macAddress: String) {
        connectionPolicy.recordFailure(macAddress)
        analytics.recordConnectionFailed()
        cache.get(macAddress)?.let {
            it.failedAttempts++
            PeerLifecycleManager.transition(it, PeerLifecycleState.DISCONNECTED)
        }
    }
}
