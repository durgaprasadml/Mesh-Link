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
        val wifiP2pGroupTimeoutMs: Long = WifiConfig.P2P_GROUP_TIMEOUT_MS
    )

    fun updateConfig(modifier: (RuntimeConfig) -> RuntimeConfig) {
        _currentConfig.value = modifier(_currentConfig.value)
    }
}
