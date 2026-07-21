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

        // Network - Bluetooth
        val BLE_ENABLED = booleanPreferencesKey("ble_enabled")
        val BLE_ADVERTISING_ENABLED = booleanPreferencesKey("ble_advertising_enabled")
        val BLE_SCANNING_ENABLED = booleanPreferencesKey("ble_scanning_enabled")
        val BLE_TX_POWER = intPreferencesKey("ble_tx_power")
        val BLE_SCAN_INTERVAL = longPreferencesKey("ble_scan_interval")
        val BLE_AUTO_RESTART = booleanPreferencesKey("ble_auto_restart")

        // Network - WiFi Direct
        val WIFI_DIRECT_ENABLED = booleanPreferencesKey("wifi_direct_enabled")
        val WIFI_AUTO_CONNECT = booleanPreferencesKey("wifi_auto_connect")
        val WIFI_PEER_DISCOVERY = booleanPreferencesKey("wifi_peer_discovery")
        val WIFI_PREFERRED_GO = booleanPreferencesKey("wifi_preferred_go")
        val WIFI_RECONNECT = booleanPreferencesKey("wifi_reconnect")

        // Transport Mode
        val PREFERRED_TRANSPORT = stringPreferencesKey("preferred_transport")

        // Relay
        val MESH_RELAY_ENABLED = booleanPreferencesKey("mesh_relay_enabled")
        val MESH_MAX_HOPS = intPreferencesKey("mesh_max_hops")
        val MESH_TTL = intPreferencesKey("mesh_ttl")
        val MESH_PRIORITY = intPreferencesKey("mesh_priority")
        val MESH_QUEUE_SIZE = intPreferencesKey("mesh_queue_size")

        // Advanced
        val ADVANCED_ENCRYPTION_ENFORCEMENT = booleanPreferencesKey("advanced_encryption_enforcement")

        // Appearance
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU_ENABLED = booleanPreferencesKey("material_you_enabled")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val GLASS_EFFECTS_ENABLED = booleanPreferencesKey("glass_effects_enabled")
        val CORNER_RADIUS_SCALE = floatPreferencesKey("corner_radius_scale")
        val LARGE_TEXT_ENABLED = booleanPreferencesKey("large_text_enabled")
        val REDUCE_MOTION_ENABLED = booleanPreferencesKey("reduce_motion_enabled")
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

    // Bluetooth
    override val isBleEnabled: Flow<Boolean> = dataStore.data.map { it[BLE_ENABLED] ?: true }
    override suspend fun setBleEnabled(enabled: Boolean) {
        dataStore.edit { it[BLE_ENABLED] = enabled }
    }
    override val bleAdvertisingEnabled: Flow<Boolean> = dataStore.data.map { it[BLE_ADVERTISING_ENABLED] ?: true }
    override suspend fun setBleAdvertisingEnabled(enabled: Boolean) {
        dataStore.edit { it[BLE_ADVERTISING_ENABLED] = enabled }
    }
    override val bleScanningEnabled: Flow<Boolean> = dataStore.data.map { it[BLE_SCANNING_ENABLED] ?: true }
    override suspend fun setBleScanningEnabled(enabled: Boolean) {
        dataStore.edit { it[BLE_SCANNING_ENABLED] = enabled }
    }
    override val bleTxPower: Flow<Int> = dataStore.data.map { it[BLE_TX_POWER] ?: 2 }
    override suspend fun setBleTxPower(power: Int) {
        dataStore.edit { it[BLE_TX_POWER] = power }
    }
    override val bleScanInterval: Flow<Long> = dataStore.data.map { it[BLE_SCAN_INTERVAL] ?: 5000L }
    override suspend fun setBleScanInterval(interval: Long) {
        dataStore.edit { it[BLE_SCAN_INTERVAL] = interval }
    }
    override val bleAutoRestart: Flow<Boolean> = dataStore.data.map { it[BLE_AUTO_RESTART] ?: true }
    override suspend fun setBleAutoRestart(enabled: Boolean) {
        dataStore.edit { it[BLE_AUTO_RESTART] = enabled }
    }

    // WiFi Direct
    override val isWifiDirectEnabled: Flow<Boolean> = dataStore.data.map { it[WIFI_DIRECT_ENABLED] ?: true }
    override suspend fun setWifiDirectEnabled(enabled: Boolean) {
        dataStore.edit { it[WIFI_DIRECT_ENABLED] = enabled }
    }
    override val wifiAutoConnect: Flow<Boolean> = dataStore.data.map { it[WIFI_AUTO_CONNECT] ?: true }
    override suspend fun setWifiAutoConnect(enabled: Boolean) {
        dataStore.edit { it[WIFI_AUTO_CONNECT] = enabled }
    }
    override val wifiPeerDiscoveryEnabled: Flow<Boolean> = dataStore.data.map { it[WIFI_PEER_DISCOVERY] ?: true }
    override suspend fun setWifiPeerDiscoveryEnabled(enabled: Boolean) {
        dataStore.edit { it[WIFI_PEER_DISCOVERY] = enabled }
    }
    override val wifiPreferredGroupOwner: Flow<Boolean> = dataStore.data.map { it[WIFI_PREFERRED_GO] ?: false }
    override suspend fun setWifiPreferredGroupOwner(enabled: Boolean) {
        dataStore.edit { it[WIFI_PREFERRED_GO] = enabled }
    }
    override val wifiReconnectEnabled: Flow<Boolean> = dataStore.data.map { it[WIFI_RECONNECT] ?: true }
    override suspend fun setWifiReconnectEnabled(enabled: Boolean) {
        dataStore.edit { it[WIFI_RECONNECT] = enabled }
    }

    // Transport Mode
    override val preferredTransport: Flow<String> = dataStore.data.map { it[PREFERRED_TRANSPORT] ?: "HYBRID" }
    override suspend fun setPreferredTransport(transport: String) {
        dataStore.edit { it[PREFERRED_TRANSPORT] = transport }
    }

    // Relay
    override val isMeshRelayEnabled: Flow<Boolean> = dataStore.data.map { it[MESH_RELAY_ENABLED] ?: true }
    override suspend fun setMeshRelayEnabled(enabled: Boolean) {
        dataStore.edit { it[MESH_RELAY_ENABLED] = enabled }
    }
    override val meshMaxHops: Flow<Int> = dataStore.data.map { it[MESH_MAX_HOPS] ?: 5 }
    override suspend fun setMeshMaxHops(hops: Int) {
        dataStore.edit { it[MESH_MAX_HOPS] = hops }
    }
    override val meshTtl: Flow<Int> = dataStore.data.map { it[MESH_TTL] ?: 10 }
    override suspend fun setMeshTtl(ttl: Int) {
        dataStore.edit { it[MESH_TTL] = ttl }
    }
    override val meshPriority: Flow<Int> = dataStore.data.map { it[MESH_PRIORITY] ?: 1 }
    override suspend fun setMeshPriority(priority: Int) {
        dataStore.edit { it[MESH_PRIORITY] = priority }
    }
    override val meshQueueSize: Flow<Int> = dataStore.data.map { it[MESH_QUEUE_SIZE] ?: 1000 }
    override suspend fun setMeshQueueSize(size: Int) {
        dataStore.edit { it[MESH_QUEUE_SIZE] = size }
    }

    // Advanced
    override val advancedEncryptionEnforcement: Flow<Boolean> = dataStore.data.map { it[ADVANCED_ENCRYPTION_ENFORCEMENT] ?: true }
    override suspend fun setAdvancedEncryptionEnforcement(enabled: Boolean) {
        dataStore.edit { it[ADVANCED_ENCRYPTION_ENFORCEMENT] = enabled }
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

    override val accentColor: Flow<String> = dataStore.data.map { it[ACCENT_COLOR] ?: "Blue" }
    override suspend fun setAccentColor(color: String) {
        dataStore.edit { it[ACCENT_COLOR] = color }
    }

    override val animationsEnabled: Flow<Boolean> = dataStore.data.map { it[ANIMATIONS_ENABLED] ?: true }
    override suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[ANIMATIONS_ENABLED] = enabled }
    }

    override val glassEffectsEnabled: Flow<Boolean> = dataStore.data.map { it[GLASS_EFFECTS_ENABLED] ?: true }
    override suspend fun setGlassEffectsEnabled(enabled: Boolean) {
        dataStore.edit { it[GLASS_EFFECTS_ENABLED] = enabled }
    }

    override val cornerRadiusScale: Flow<Float> = dataStore.data.map { it[CORNER_RADIUS_SCALE] ?: 1.0f }
    override suspend fun setCornerRadiusScale(scale: Float) {
        dataStore.edit { it[CORNER_RADIUS_SCALE] = scale }
    }

    override val largeTextEnabled: Flow<Boolean> = dataStore.data.map { it[LARGE_TEXT_ENABLED] ?: false }
    override suspend fun setLargeTextEnabled(enabled: Boolean) {
        dataStore.edit { it[LARGE_TEXT_ENABLED] = enabled }
    }

    override val reduceMotionEnabled: Flow<Boolean> = dataStore.data.map { it[REDUCE_MOTION_ENABLED] ?: false }
    override suspend fun setReduceMotionEnabled(enabled: Boolean) {
        dataStore.edit { it[REDUCE_MOTION_ENABLED] = enabled }
    }
}
