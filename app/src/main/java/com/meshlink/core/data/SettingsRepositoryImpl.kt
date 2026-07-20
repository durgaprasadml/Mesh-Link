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

    override val autoLockTimeoutMs: Flow<Long> = localDataSource.autoLockTimeoutMs
    override suspend fun setAutoLockTimeoutMs(timeoutMs: Long) {
        localDataSource.setAutoLockTimeoutMs(timeoutMs)
    }

    override val isBiometricsEnabled: Flow<Boolean> = localDataSource.isBiometricsEnabled
    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        localDataSource.setBiometricsEnabled(enabled)
    }

    // Network
    override val isBleEnabled: Flow<Boolean> = localDataSource.isBleEnabled
    override suspend fun setBleEnabled(enabled: Boolean) {
        localDataSource.setBleEnabled(enabled)
    }

    override val isWifiDirectEnabled: Flow<Boolean> = localDataSource.isWifiDirectEnabled
    override suspend fun setWifiDirectEnabled(enabled: Boolean) {
        localDataSource.setWifiDirectEnabled(enabled)
    }

    override val preferredTransport: Flow<String> = localDataSource.preferredTransport
    override suspend fun setPreferredTransport(transport: String) {
        localDataSource.setPreferredTransport(transport)
    }

    override val isMeshRelayEnabled: Flow<Boolean> = localDataSource.isMeshRelayEnabled
    override suspend fun setMeshRelayEnabled(enabled: Boolean) {
        localDataSource.setMeshRelayEnabled(enabled)
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
