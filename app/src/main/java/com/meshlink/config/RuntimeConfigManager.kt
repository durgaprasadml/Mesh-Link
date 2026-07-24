package com.meshlink.config

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class RuntimeConfigManager @Inject constructor() {
    
    private val _currentConfig = MutableStateFlow(RuntimeConfig())
    val currentConfig: StateFlow<RuntimeConfig> = _currentConfig

    data class RuntimeConfig(
        val routingRetryCount: Int = MeshConfig.ROUTING_RETRY_COUNT,
        val maxRelayPackets: Int = MeshConfig.MAX_RELAY_PACKETS,
        val defaultTtl: Int = MeshConfig.DEFAULT_TTL,
        val bleScanIntervalMs: Long = BleConfig.SCAN_INTERVAL_MS,
        val wifiP2pGroupTimeoutMs: Long = WifiConfig.P2P_GROUP_TIMEOUT_MS,
        
        // Dynamic Routing Configurations
        val routeTimeoutMs: Long = 5 * 60 * 1000L, // 5 minutes route expiration
        val duplicateCacheSize: Int = 20000,
        val duplicateCacheLifetimeMs: Long = 10 * 60 * 1000L, // 10 minutes cache expiration
        val emaAlpha: Float = 0.3f, // EMA smoothing factor for RSSI (0.3 responds well to recent changes without flapping)
        val routeSwitchingThreshold: Int = 10, // Score diff required to switch stable routes
        val maxHopLimit: Int = 15 // Global mesh hop limit
    )

    fun updateConfig(modifier: (RuntimeConfig) -> RuntimeConfig) {
        _currentConfig.value = modifier(_currentConfig.value)
    }
}
