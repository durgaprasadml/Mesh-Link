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

    // Network
    val isBleEnabled: Flow<Boolean>
    suspend fun setBleEnabled(enabled: Boolean)

    val isWifiDirectEnabled: Flow<Boolean>
    suspend fun setWifiDirectEnabled(enabled: Boolean)

    val preferredTransport: Flow<String> // "BLE", "WIFI", "HYBRID"
    suspend fun setPreferredTransport(transport: String)

    val isMeshRelayEnabled: Flow<Boolean>
    suspend fun setMeshRelayEnabled(enabled: Boolean)

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
