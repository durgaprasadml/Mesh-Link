package com.meshlink.core.data

import com.meshlink.core.data.source.SettingsLocalDataSource
import com.meshlink.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl @Inject constructor(
    private val localDataSource: SettingsLocalDataSource
) : SettingsRepository {

    // Security settings removed

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

    // Advanced
    override val advancedEncryptionEnforcement: Flow<Boolean> = localDataSource.advancedEncryptionEnforcement
    override suspend fun setAdvancedEncryptionEnforcement(enabled: Boolean) {
        localDataSource.setAdvancedEncryptionEnforcement(enabled)
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

    override val accentColor: Flow<String> = localDataSource.accentColor
    override suspend fun setAccentColor(color: String) {
        localDataSource.setAccentColor(color)
    }

    override val animationsEnabled: Flow<Boolean> = localDataSource.animationsEnabled
    override suspend fun setAnimationsEnabled(enabled: Boolean) {
        localDataSource.setAnimationsEnabled(enabled)
    }

    override val glassEffectsEnabled: Flow<Boolean> = localDataSource.glassEffectsEnabled
    override suspend fun setGlassEffectsEnabled(enabled: Boolean) {
        localDataSource.setGlassEffectsEnabled(enabled)
    }

    override val cornerRadiusScale: Flow<Float> = localDataSource.cornerRadiusScale
    override suspend fun setCornerRadiusScale(scale: Float) {
        localDataSource.setCornerRadiusScale(scale)
    }

    override val largeTextEnabled: Flow<Boolean> = localDataSource.largeTextEnabled
    override suspend fun setLargeTextEnabled(enabled: Boolean) {
        localDataSource.setLargeTextEnabled(enabled)
    }

    override val reduceMotionEnabled: Flow<Boolean> = localDataSource.reduceMotionEnabled
    override suspend fun setReduceMotionEnabled(enabled: Boolean) {
        localDataSource.setReduceMotionEnabled(enabled)
    }
}
