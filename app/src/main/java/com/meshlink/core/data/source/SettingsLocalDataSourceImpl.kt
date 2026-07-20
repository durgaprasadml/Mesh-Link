package com.meshlink.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsLocalDataSource {

    companion object {
        // Security
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val AUTO_LOCK_TIMEOUT_MS = longPreferencesKey("auto_lock_timeout_ms")
        val BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")

        // Network
        val BLE_ENABLED = booleanPreferencesKey("ble_enabled")
        val WIFI_DIRECT_ENABLED = booleanPreferencesKey("wifi_direct_enabled")
        val PREFERRED_TRANSPORT = stringPreferencesKey("preferred_transport")
        val MESH_RELAY_ENABLED = booleanPreferencesKey("mesh_relay_enabled")

        // Appearance
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU_ENABLED = booleanPreferencesKey("material_you_enabled")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    }

    // Security
    override val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { it[APP_LOCK_ENABLED] ?: false }
    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    override val appLockPinHash: Flow<String?> = dataStore.data.map { it[APP_LOCK_PIN_HASH] }
    override suspend fun setAppLockPinHash(pinHash: String?) {
        dataStore.edit { prefs ->
            if (pinHash == null) prefs.remove(APP_LOCK_PIN_HASH)
            else prefs[APP_LOCK_PIN_HASH] = pinHash
        }
    }

    override val autoLockTimeoutMs: Flow<Long> = dataStore.data.map { it[AUTO_LOCK_TIMEOUT_MS] ?: 60000L }
    override suspend fun setAutoLockTimeoutMs(timeoutMs: Long) {
        dataStore.edit { it[AUTO_LOCK_TIMEOUT_MS] = timeoutMs }
    }

    override val isBiometricsEnabled: Flow<Boolean> = dataStore.data.map { it[BIOMETRICS_ENABLED] ?: false }
    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        dataStore.edit { it[BIOMETRICS_ENABLED] = enabled }
    }

    // Network
    override val isBleEnabled: Flow<Boolean> = dataStore.data.map { it[BLE_ENABLED] ?: true }
    override suspend fun setBleEnabled(enabled: Boolean) {
        dataStore.edit { it[BLE_ENABLED] = enabled }
    }

    override val isWifiDirectEnabled: Flow<Boolean> = dataStore.data.map { it[WIFI_DIRECT_ENABLED] ?: true }
    override suspend fun setWifiDirectEnabled(enabled: Boolean) {
        dataStore.edit { it[WIFI_DIRECT_ENABLED] = enabled }
    }

    override val preferredTransport: Flow<String> = dataStore.data.map { it[PREFERRED_TRANSPORT] ?: "HYBRID" }
    override suspend fun setPreferredTransport(transport: String) {
        dataStore.edit { it[PREFERRED_TRANSPORT] = transport }
    }

    override val isMeshRelayEnabled: Flow<Boolean> = dataStore.data.map { it[MESH_RELAY_ENABLED] ?: true }
    override suspend fun setMeshRelayEnabled(enabled: Boolean) {
        dataStore.edit { it[MESH_RELAY_ENABLED] = enabled }
    }

    // Appearance
    override val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "SYSTEM" }
    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    override val isMaterialYouEnabled: Flow<Boolean> = dataStore.data.map { it[MATERIAL_YOU_ENABLED] ?: true }
    override suspend fun setMaterialYouEnabled(enabled: Boolean) {
        dataStore.edit { it[MATERIAL_YOU_ENABLED] = enabled }
    }

    override val fontScale: Flow<Float> = dataStore.data.map { it[FONT_SCALE] ?: 1.0f }
    override suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[FONT_SCALE] = scale }
    }

    override val highContrast: Flow<Boolean> = dataStore.data.map { it[HIGH_CONTRAST] ?: false }
    override suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { it[HIGH_CONTRAST] = enabled }
    }
}
