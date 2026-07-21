package com.meshlink.core.data

import com.meshlink.core.data.source.SettingsLocalDataSource
import com.meshlink.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl @Inject constructor(
    private val localDataSource: SettingsLocalDataSource
) : SettingsRepository {

    // Security
    override val isAppLockEnabled: Flow<Boolean> = localDataSource.isAppLockEnabled
    override suspend fun setAppLockEnabled(enabled: Boolean) {
        localDataSource.setAppLockEnabled(enabled)
    }

    override val appLockPinHash: Flow<String?> = localDataSource.appLockPinHash
    override suspend fun setAppLockPinHash(pinHash: String?) {
        localDataSource.setAppLockPinHash(pinHash)
    }

    override val autoLockTimeoutMs: Flow<Long> = localDataSource.autoLockTimeoutMs
    override suspend fun setAutoLockTimeoutMs(timeoutMs: Long) {
        localDataSource.setAutoLockTimeoutMs(timeoutMs)
    }

    override val isBiometricsEnabled: Flow<Boolean> = localDataSource.isBiometricsEnabled
    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        localDataSource.setBiometricsEnabled(enabled)
    }

    // Bluetooth
    override val isBleEnabled: Flow<Boolean> = localDataSource.isBleEnabled
    override suspend fun setBleEnabled(enabled: Boolean) {
        localDataSource.setBleEnabled(enabled)
    }
    override val bleAdvertisingEnabled: Flow<Boolean> = localDataSource.bleAdvertisingEnabled
    override suspend fun setBleAdvertisingEnabled(enabled: Boolean) {
        localDataSource.setBleAdvertisingEnabled(enabled)
    }
    override val bleScanningEnabled: Flow<Boolean> = localDataSource.bleScanningEnabled
    override suspend fun setBleScanningEnabled(enabled: Boolean) {
        localDataSource.setBleScanningEnabled(enabled)
    }
    override val bleTxPower: Flow<Int> = localDataSource.bleTxPower
    override suspend fun setBleTxPower(power: Int) {
        localDataSource.setBleTxPower(power)
    }
    override val bleScanInterval: Flow<Long> = localDataSource.bleScanInterval
    override suspend fun setBleScanInterval(interval: Long) {
        localDataSource.setBleScanInterval(interval)
    }
    override val bleAutoRestart: Flow<Boolean> = localDataSource.bleAutoRestart
    override suspend fun setBleAutoRestart(enabled: Boolean) {
        localDataSource.setBleAutoRestart(enabled)
    }

    // WiFi Direct
    override val isWifiDirectEnabled: Flow<Boolean> = localDataSource.isWifiDirectEnabled
    override suspend fun setWifiDirectEnabled(enabled: Boolean) {
        localDataSource.setWifiDirectEnabled(enabled)
    }
    override val wifiAutoConnect: Flow<Boolean> = localDataSource.wifiAutoConnect
    override suspend fun setWifiAutoConnect(enabled: Boolean) {
        localDataSource.setWifiAutoConnect(enabled)
    }
    override val wifiPeerDiscoveryEnabled: Flow<Boolean> = localDataSource.wifiPeerDiscoveryEnabled
    override suspend fun setWifiPeerDiscoveryEnabled(enabled: Boolean) {
        localDataSource.setWifiPeerDiscoveryEnabled(enabled)
    }
    override val wifiPreferredGroupOwner: Flow<Boolean> = localDataSource.wifiPreferredGroupOwner
    override suspend fun setWifiPreferredGroupOwner(enabled: Boolean) {
        localDataSource.setWifiPreferredGroupOwner(enabled)
    }
    override val wifiReconnectEnabled: Flow<Boolean> = localDataSource.wifiReconnectEnabled
    override suspend fun setWifiReconnectEnabled(enabled: Boolean) {
        localDataSource.setWifiReconnectEnabled(enabled)
    }

    // Transport Mode
    override val preferredTransport: Flow<String> = localDataSource.preferredTransport
    override suspend fun setPreferredTransport(transport: String) {
        localDataSource.setPreferredTransport(transport)
    }

    // Relay
    override val isMeshRelayEnabled: Flow<Boolean> = localDataSource.isMeshRelayEnabled
    override suspend fun setMeshRelayEnabled(enabled: Boolean) {
        localDataSource.setMeshRelayEnabled(enabled)
    }
    override val meshMaxHops: Flow<Int> = localDataSource.meshMaxHops
    override suspend fun setMeshMaxHops(hops: Int) {
        localDataSource.setMeshMaxHops(hops)
    }
    override val meshTtl: Flow<Int> = localDataSource.meshTtl
    override suspend fun setMeshTtl(ttl: Int) {
        localDataSource.setMeshTtl(ttl)
    }
    override val meshPriority: Flow<Int> = localDataSource.meshPriority
    override suspend fun setMeshPriority(priority: Int) {
        localDataSource.setMeshPriority(priority)
    }
    override val meshQueueSize: Flow<Int> = localDataSource.meshQueueSize
    override suspend fun setMeshQueueSize(size: Int) {
        localDataSource.setMeshQueueSize(size)
    }

    // Discovery
    override val discoveryInterval: Flow<Long> = localDataSource.discoveryInterval
    override suspend fun setDiscoveryInterval(interval: Long) {
        localDataSource.setDiscoveryInterval(interval)
    }
    override val discoveryBackground: Flow<Boolean> = localDataSource.discoveryBackground
    override suspend fun setDiscoveryBackground(enabled: Boolean) {
        localDataSource.setDiscoveryBackground(enabled)
    }
    override val discoveryForeground: Flow<Boolean> = localDataSource.discoveryForeground
    override suspend fun setDiscoveryForeground(enabled: Boolean) {
        localDataSource.setDiscoveryForeground(enabled)
    }
    override val discoveryTimeout: Flow<Long> = localDataSource.discoveryTimeout
    override suspend fun setDiscoveryTimeout(timeout: Long) {
        localDataSource.setDiscoveryTimeout(timeout)
    }
    override val discoveryRestart: Flow<Boolean> = localDataSource.discoveryRestart
    override suspend fun setDiscoveryRestart(enabled: Boolean) {
        localDataSource.setDiscoveryRestart(enabled)
    }

    // Advanced
    override val advancedPacketSize: Flow<Int> = localDataSource.advancedPacketSize
    override suspend fun setAdvancedPacketSize(size: Int) {
        localDataSource.setAdvancedPacketSize(size)
    }
    override val advancedRetryCount: Flow<Int> = localDataSource.advancedRetryCount
    override suspend fun setAdvancedRetryCount(count: Int) {
        localDataSource.setAdvancedRetryCount(count)
    }
    override val advancedCompression: Flow<Boolean> = localDataSource.advancedCompression
    override suspend fun setAdvancedCompression(enabled: Boolean) {
        localDataSource.setAdvancedCompression(enabled)
    }
    override val advancedEncryptionEnforcement: Flow<Boolean> = localDataSource.advancedEncryptionEnforcement
    override suspend fun setAdvancedEncryptionEnforcement(enabled: Boolean) {
        localDataSource.setAdvancedEncryptionEnforcement(enabled)
    }
    override val advancedBandwidthOptimization: Flow<Boolean> = localDataSource.advancedBandwidthOptimization
    override suspend fun setAdvancedBandwidthOptimization(enabled: Boolean) {
        localDataSource.setAdvancedBandwidthOptimization(enabled)
    }

    // Appearance
    override val themeMode: Flow<String> = localDataSource.themeMode
    override suspend fun setThemeMode(mode: String) {
        localDataSource.setThemeMode(mode)
    }

    override val isMaterialYouEnabled: Flow<Boolean> = localDataSource.isMaterialYouEnabled
    override suspend fun setMaterialYouEnabled(enabled: Boolean) {
        localDataSource.setMaterialYouEnabled(enabled)
    }

    override val fontScale: Flow<Float> = localDataSource.fontScale
    override suspend fun setFontScale(scale: Float) {
        localDataSource.setFontScale(scale)
    }

    override val highContrast: Flow<Boolean> = localDataSource.highContrast
    override suspend fun setHighContrast(enabled: Boolean) {
        localDataSource.setHighContrast(enabled)
    }
}
