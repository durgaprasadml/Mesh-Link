package com.meshlink.core.data.source

import kotlinx.coroutines.flow.Flow

interface SettingsLocalDataSource {
    // Security
    val isAppLockEnabled: Flow<Boolean>
    suspend fun setAppLockEnabled(enabled: Boolean)

    val appLockPinHash: Flow<String?>
    suspend fun setAppLockPinHash(pinHash: String?)

    val autoLockTimeoutMs: Flow<Long>
    suspend fun setAutoLockTimeoutMs(timeoutMs: Long)

    val isBiometricsEnabled: Flow<Boolean>
    suspend fun setBiometricsEnabled(enabled: Boolean)

    // Network & Transport Settings
    // Bluetooth
    val isBleEnabled: Flow<Boolean>
    suspend fun setBleEnabled(enabled: Boolean)
    
    val bleAdvertisingEnabled: Flow<Boolean>
    suspend fun setBleAdvertisingEnabled(enabled: Boolean)
    
    val bleScanningEnabled: Flow<Boolean>
    suspend fun setBleScanningEnabled(enabled: Boolean)
    
    val bleTxPower: Flow<Int>
    suspend fun setBleTxPower(power: Int)
    
    val bleScanInterval: Flow<Long>
    suspend fun setBleScanInterval(interval: Long)
    
    val bleAutoRestart: Flow<Boolean>
    suspend fun setBleAutoRestart(enabled: Boolean)

    // WiFi Direct
    val isWifiDirectEnabled: Flow<Boolean>
    suspend fun setWifiDirectEnabled(enabled: Boolean)

    val wifiAutoConnect: Flow<Boolean>
    suspend fun setWifiAutoConnect(enabled: Boolean)
    
    val wifiPeerDiscoveryEnabled: Flow<Boolean>
    suspend fun setWifiPeerDiscoveryEnabled(enabled: Boolean)
    
    val wifiPreferredGroupOwner: Flow<Boolean>
    suspend fun setWifiPreferredGroupOwner(enabled: Boolean)
    
    val wifiReconnectEnabled: Flow<Boolean>
    suspend fun setWifiReconnectEnabled(enabled: Boolean)

    // Transport Mode
    val preferredTransport: Flow<String> // "BLE", "WIFI", "HYBRID", "AUTOMATIC"
    suspend fun setPreferredTransport(transport: String)

    // Relay
    val isMeshRelayEnabled: Flow<Boolean>
    suspend fun setMeshRelayEnabled(enabled: Boolean)
    
    val meshMaxHops: Flow<Int>
    suspend fun setMeshMaxHops(hops: Int)
    
    val meshTtl: Flow<Int>
    suspend fun setMeshTtl(ttl: Int)
    
    val meshPriority: Flow<Int>
    suspend fun setMeshPriority(priority: Int)
    
    val meshQueueSize: Flow<Int>
    suspend fun setMeshQueueSize(size: Int)

    // Discovery
    val discoveryInterval: Flow<Long>
    suspend fun setDiscoveryInterval(interval: Long)
    
    val discoveryBackground: Flow<Boolean>
    suspend fun setDiscoveryBackground(enabled: Boolean)
    
    val discoveryForeground: Flow<Boolean>
    suspend fun setDiscoveryForeground(enabled: Boolean)
    
    val discoveryTimeout: Flow<Long>
    suspend fun setDiscoveryTimeout(timeout: Long)
    
    val discoveryRestart: Flow<Boolean>
    suspend fun setDiscoveryRestart(enabled: Boolean)

    // Advanced
    val advancedPacketSize: Flow<Int>
    suspend fun setAdvancedPacketSize(size: Int)
    
    val advancedRetryCount: Flow<Int>
    suspend fun setAdvancedRetryCount(count: Int)
    
    val advancedCompression: Flow<Boolean>
    suspend fun setAdvancedCompression(enabled: Boolean)
    
    val advancedEncryptionEnforcement: Flow<Boolean>
    suspend fun setAdvancedEncryptionEnforcement(enabled: Boolean)
    
    val advancedBandwidthOptimization: Flow<Boolean>
    suspend fun setAdvancedBandwidthOptimization(enabled: Boolean)

    // Appearance
    val themeMode: Flow<String> // "SYSTEM", "LIGHT", "DARK"
    suspend fun setThemeMode(mode: String)

    val isMaterialYouEnabled: Flow<Boolean>
    suspend fun setMaterialYouEnabled(enabled: Boolean)

    val fontScale: Flow<Float>
    suspend fun setFontScale(scale: Float)

    val highContrast: Flow<Boolean>
    suspend fun setHighContrast(enabled: Boolean)
}
