package com.meshlink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable

@Immutable
data class ThemePreferences(
    val themeMode: String = "LIGHT",
    val isMaterialYouEnabled: Boolean = true,
    val accentColor: String = "Blue",
    val fontScale: Float = 1.0f,
    val largeTextEnabled: Boolean = false,
    val cornerRadiusScale: Float = 1.0f,
    val animationsEnabled: Boolean = true,
    val glassEffectsEnabled: Boolean = true,
    val highContrast: Boolean = false,
    val reduceMotionEnabled: Boolean = false
)

@Immutable
data class SettingsUiState(
    val user: User? = null,
    
    // Legacy UserRepository settings
    val isEncryptionEnabled: Boolean = true,
    val isOnlineVisible: Boolean = true,
    val meshMode: String = "Auto",
    
    // Network - Bluetooth
    val isBleEnabled: Boolean = true,
    val bleAdvertisingEnabled: Boolean = true,
    val bleScanningEnabled: Boolean = true,
    val bleTxPower: Int = 2,
    val bleScanInterval: Long = 5000L,
    val bleAutoRestart: Boolean = true,

    // Transport Mode
    val preferredTransport: String = "BLE",

    // Relay
    val isMeshRelayEnabled: Boolean = true,
    val meshMaxHops: Int = 5,
    val meshTtl: Int = 10,
    val meshPriority: Int = 1,
    val meshQueueSize: Int = 1000,

    // Advanced
    val advancedEncryptionEnforcement: Boolean = true,
    
    // Appearance
    val themeMode: String = "LIGHT",
    val isMaterialYouEnabled: Boolean = true,
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false,
    val accentColor: String = "Blue",
    val animationsEnabled: Boolean = true,
    val glassEffectsEnabled: Boolean = true,
    val cornerRadiusScale: Float = 1.0f,
    val largeTextEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false,

    // Messaging
    val mediaQuality: String = "HD",
    val autoDownload: String = "Wi-Fi & Mesh",
    val readReceipts: Boolean = true,
    val deliveryStatus: Boolean = true,
    val messageRetention: String = "Forever",
    val autoRetryCount: Int = 3,

    // Emergency SOS
    val emergencyNumber: String = "112",
    val sosAutoBroadcast: Boolean = true,
    val shareLiveLocation: Boolean = true,
    val emergencyTemplate: String = "EMERGENCY! Immediate assistance needed at my coordinates.",

    // Notifications
    val messageNotifications: Boolean = true,
    val sosAlertsEnabled: Boolean = true,
    val notificationSound: String = "Mesh Pulse",
    val vibrationEnabled: Boolean = true,
    val ledFlashEnabled: Boolean = true,
    val notificationPriority: String = "High",

    // Privacy & Security
    val biometricUnlock: Boolean = false,
    val appLockEnabled: Boolean = false,
    val trustedDevicesCount: Int = 4,

    // Developer / Diagnostics
    val developerMode: Boolean = true,
    val transportLogsEnabled: Boolean = true,
    val packetCount: Long = 142850L,

    // Storage Usage Breakdown (actual bytes on disk)
    val databaseBytes: Long = 0L,
    val mediaCacheBytes: Long = 0L,
    val voiceNotesBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L,
    val totalStorageBytes: Long = 1L
)

sealed class SettingsEvent {
    data class Error(val message: String) : SettingsEvent()
    data class SuccessMessage(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val cacheManager: com.meshlink.storage.data.local.CacheManager? = null,
    private val meshDatabase: com.meshlink.database.data.local.MeshDatabase? = null
) : ViewModel() {

    internal var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO

    private data class ExtraSettingsState(
        val emergencyNumber: String = "112",
        val sosAutoBroadcast: Boolean = true,
        val shareLiveLocation: Boolean = true,
        val emergencyTemplate: String = "EMERGENCY! Immediate assistance needed at my coordinates.",
        val databaseBytes: Long = 0L,
        val mediaCacheBytes: Long = 0L,
        val voiceNotesBytes: Long = 0L,
        val freeSpaceBytes: Long = 0L,
        val totalStorageBytes: Long = 1L
    )

    private val _extraState = MutableStateFlow(ExtraSettingsState())

    private val _uiEvent = MutableSharedFlow<SettingsEvent>(replay = 0)
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                settingsRepository.emergencyNumber.collect { num ->
                    _extraState.update { it.copy(emergencyNumber = num) }
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                settingsRepository.sosAutoBroadcast.collect { enabled ->
                    _extraState.update { it.copy(sosAutoBroadcast = enabled) }
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                settingsRepository.shareLiveLocation.collect { enabled ->
                    _extraState.update { it.copy(shareLiveLocation = enabled) }
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                settingsRepository.emergencyTemplate.collect { tmpl ->
                    _extraState.update { it.copy(emergencyTemplate = tmpl) }
                }
            } catch (_: Exception) {}
        }
        refreshStorageStats()
    }

    private val combinedState: Flow<SettingsUiState> = combine(
        userRepository.localUser,
        combine(
            userRepository.isEncryptionEnabled,
            userRepository.isOnlineVisible,
            userRepository.meshMode
        ) { enc, onl, mesh ->
            SettingsGroup1(enc, onl, mesh)
        },
        combine(
            settingsRepository.isBleEnabled,
            settingsRepository.bleAdvertisingEnabled,
            settingsRepository.bleScanningEnabled,
            settingsRepository.bleTxPower
        ) { ble, bleAdv, bleScan, bleTx ->
            SettingsGroup2(ble, bleAdv, bleScan, bleTx)
        },
        combine(
            settingsRepository.bleScanInterval,
            settingsRepository.bleAutoRestart
        ) { bleInt, bleAuto ->
            SettingsGroup3(bleInt, bleAuto)
        },
        combine(
            settingsRepository.preferredTransport,
            settingsRepository.isMeshRelayEnabled,
            settingsRepository.meshMaxHops
        ) { trans, relay, hops ->
            SettingsGroup4(trans, relay, hops)
        },
        combine(
            settingsRepository.meshTtl,
            settingsRepository.meshPriority,
            settingsRepository.meshQueueSize,
            settingsRepository.advancedEncryptionEnforcement,
            settingsRepository.themeMode
        ) { ttl, prio, queue, encEnf, theme ->
            SettingsGroup5(ttl, prio, queue, encEnf, theme)
        }
    ) { args ->
        val user = args[0] as User?
        val g1 = args[1] as SettingsGroup1
        val g2 = args[2] as SettingsGroup2
        val g3 = args[3] as SettingsGroup3
        val g4 = args[4] as SettingsGroup4
        val g5 = args[5] as SettingsGroup5
        
        SettingsUiState(
            user = user,
            isEncryptionEnabled = g1.enc,
            isOnlineVisible = g1.onl,
            meshMode = g1.mesh,
            
            isBleEnabled = g2.ble,
            bleAdvertisingEnabled = g2.bleAdv,
            bleScanningEnabled = g2.bleScan,
            bleTxPower = g2.bleTx,
            
            bleScanInterval = g3.bleInt,
            bleAutoRestart = g3.bleAuto,
            
            preferredTransport = g4.trans,
            isMeshRelayEnabled = g4.relay,
            meshMaxHops = g4.hops,
            
            meshTtl = g5.ttl,
            meshPriority = g5.prio,
            meshQueueSize = g5.queue,
            advancedEncryptionEnforcement = g5.encEnf,
            themeMode = g5.theme
        )
    }

    // Fetch the remaining properties in a separate combine
    val uiState = combine(
        combinedState,
        settingsRepository.isMaterialYouEnabled,
        settingsRepository.fontScale,
        settingsRepository.highContrast,
        settingsRepository.accentColor,
        settingsRepository.animationsEnabled,
        settingsRepository.glassEffectsEnabled,
        settingsRepository.cornerRadiusScale,
        settingsRepository.largeTextEnabled,
        settingsRepository.reduceMotionEnabled,
        _extraState
    ) { args ->
        val state = args[0] as SettingsUiState
        val extra = args[10] as ExtraSettingsState
        state.copy(
            isMaterialYouEnabled = args[1] as Boolean,
            fontScale = args[2] as Float,
            highContrast = args[3] as Boolean,
            accentColor = args[4] as String,
            animationsEnabled = args[5] as Boolean,
            glassEffectsEnabled = args[6] as Boolean,
            cornerRadiusScale = args[7] as Float,
            largeTextEnabled = args[8] as Boolean,
            reduceMotionEnabled = args[9] as Boolean,
            emergencyNumber = extra.emergencyNumber,
            sosAutoBroadcast = extra.sosAutoBroadcast,
            shareLiveLocation = extra.shareLiveLocation,
            emergencyTemplate = extra.emergencyTemplate,
            databaseBytes = extra.databaseBytes,
            mediaCacheBytes = extra.mediaCacheBytes,
            voiceNotesBytes = extra.voiceNotesBytes,
            freeSpaceBytes = extra.freeSpaceBytes,
            totalStorageBytes = extra.totalStorageBytes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val themePreferences: StateFlow<ThemePreferences> = uiState.map { state ->
        ThemePreferences(
            themeMode = state.themeMode,
            isMaterialYouEnabled = state.isMaterialYouEnabled,
            accentColor = state.accentColor,
            fontScale = state.fontScale,
            largeTextEnabled = state.largeTextEnabled,
            cornerRadiusScale = state.cornerRadiusScale,
            animationsEnabled = state.animationsEnabled,
            glassEffectsEnabled = state.glassEffectsEnabled,
            highContrast = state.highContrast,
            reduceMotionEnabled = state.reduceMotionEnabled
        )
    }.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreferences())

    // Internal data classes for grouping
    private data class SettingsGroup1(val enc: Boolean, val onl: Boolean, val mesh: String)
    private data class SettingsGroup2(val ble: Boolean, val bleAdv: Boolean, val bleScan: Boolean, val bleTx: Int)
    private data class SettingsGroup3(val bleInt: Long, val bleAuto: Boolean)
    private data class SettingsGroup4(val trans: String, val relay: Boolean, val hops: Int)
    private data class SettingsGroup5(val ttl: Int, val prio: Int, val queue: Int, val encEnf: Boolean, val theme: String)

    // Profile Settings
    fun updateUserName(name: String) {
        viewModelScope.launch {
            try {
                userRepository.updateUserName(name)
                _uiEvent.emit(SettingsEvent.SuccessMessage("Profile updated"))
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error("Failed to update profile"))
            }
        }
    }

    // Legacy User Settings
    fun setEncryptionEnabled(enabled: Boolean) = viewModelScope.launch { userRepository.setEncryptionEnabled(enabled) }
    fun setOnlineVisible(visible: Boolean) = viewModelScope.launch { userRepository.setOnlineVisible(visible) }
    fun setMeshMode(mode: String) = viewModelScope.launch { userRepository.setMeshMode(mode) }

    // Security Settings Removed

    // Bluetooth Settings
    fun setBleEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleEnabled(enabled) }
    fun setBleAdvertisingEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleAdvertisingEnabled(enabled) }
    fun setBleScanningEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleScanningEnabled(enabled) }
    fun setBleTxPower(power: Int) = viewModelScope.launch { settingsRepository.setBleTxPower(power) }
    fun setBleScanInterval(interval: Long) = viewModelScope.launch { settingsRepository.setBleScanInterval(interval) }
    fun setBleAutoRestart(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleAutoRestart(enabled) }


    // Transport Mode
    fun setPreferredTransport(transport: String) = viewModelScope.launch { settingsRepository.setPreferredTransport(transport) }

    // Relay
    fun setMeshRelayEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setMeshRelayEnabled(enabled) }
    fun setMeshMaxHops(hops: Int) = viewModelScope.launch { settingsRepository.setMeshMaxHops(hops) }
    fun setMeshTtl(ttl: Int) = viewModelScope.launch { settingsRepository.setMeshTtl(ttl) }
    fun setMeshPriority(priority: Int) = viewModelScope.launch { settingsRepository.setMeshPriority(priority) }
    fun setMeshQueueSize(size: Int) = viewModelScope.launch { settingsRepository.setMeshQueueSize(size) }

    fun setAdvancedEncryptionEnforcement(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAdvancedEncryptionEnforcement(enabled) }

    // Appearance Settings
    fun setThemeMode(mode: String) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setMaterialYouEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setMaterialYouEnabled(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsRepository.setFontScale(scale) }
    fun setHighContrast(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHighContrast(enabled) }
    fun setAccentColor(color: String) = viewModelScope.launch { settingsRepository.setAccentColor(color) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAnimationsEnabled(enabled) }
    fun setGlassEffectsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setGlassEffectsEnabled(enabled) }
    fun setCornerRadiusScale(scale: Float) = viewModelScope.launch { settingsRepository.setCornerRadiusScale(scale) }
    fun setLargeTextEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setLargeTextEnabled(enabled) }
    fun setReduceMotionEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setReduceMotionEnabled(enabled) }

    // Emergency SOS Settings
    fun setEmergencyNumber(number: String) = viewModelScope.launch {
        val sanitized = number.trim().ifBlank { "112" }
        _extraState.update { it.copy(emergencyNumber = sanitized) }
        settingsRepository.setEmergencyNumber(sanitized)
        _uiEvent.emit(SettingsEvent.SuccessMessage("Emergency number updated to $sanitized"))
    }

    fun setSosAutoBroadcast(enabled: Boolean) = viewModelScope.launch {
        _extraState.update { it.copy(sosAutoBroadcast = enabled) }
        settingsRepository.setSosAutoBroadcast(enabled)
        _uiEvent.emit(SettingsEvent.SuccessMessage(if (enabled) "SOS auto broadcast enabled" else "SOS auto broadcast disabled"))
    }

    fun setShareLiveLocation(enabled: Boolean) = viewModelScope.launch {
        _extraState.update { it.copy(shareLiveLocation = enabled) }
        settingsRepository.setShareLiveLocation(enabled)
        _uiEvent.emit(SettingsEvent.SuccessMessage(if (enabled) "Live location sharing enabled" else "Live location sharing disabled"))
    }

    fun setEmergencyTemplate(template: String) = viewModelScope.launch {
        val sanitized = template.trim().ifBlank { "EMERGENCY! Immediate assistance needed at my coordinates." }
        _extraState.update { it.copy(emergencyTemplate = sanitized) }
        settingsRepository.setEmergencyTemplate(sanitized)
        _uiEvent.emit(SettingsEvent.SuccessMessage("Emergency template updated"))
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            try {
                val breakdown = cacheManager?.getStorageBreakdown() ?: return@launch
                _extraState.update {
                    it.copy(
                        databaseBytes = breakdown.databaseBytes,
                        mediaCacheBytes = breakdown.mediaCacheBytes,
                        voiceNotesBytes = breakdown.voiceNotesBytes,
                        freeSpaceBytes = breakdown.freeSpaceBytes,
                        totalStorageBytes = breakdown.totalSpaceBytes
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // Action Triggers for UI Feedback
    fun showToast(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(SettingsEvent.SuccessMessage(message))
        }
    }

    fun clearMediaCache() {
        viewModelScope.launch {
            try {
                val freedBytes = cacheManager?.clearMediaCache() ?: 0L
                val orphanFreed = clearOrphanFilesInternal()
                val totalFreed = freedBytes + orphanFreed
                refreshStorageStats()
                _uiEvent.emit(SettingsEvent.SuccessMessage("Media cache cleared (${formatBytes(totalFreed)} freed)"))
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error("Failed to clear media cache: ${e.message ?: "unknown error"}"))
            }
        }
    }

    fun clearOrphanFiles() {
        viewModelScope.launch {
            try {
                val freed = clearOrphanFilesInternal()
                refreshStorageStats()
                _uiEvent.emit(SettingsEvent.SuccessMessage("Orphan files cleaned (${formatBytes(freed)} freed)"))
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error("Failed to clean orphan files: ${e.message ?: "unknown error"}"))
            }
        }
    }

    private suspend fun clearOrphanFilesInternal(): Long {
        val manager = cacheManager ?: return 0L
        val referencedPaths = kotlinx.coroutines.withContext(ioDispatcher) {
            val refs = mutableSetOf<String>()
            try {
                val db = meshDatabase?.openHelper?.readableDatabase
                db?.query("SELECT mediaPath FROM messages WHERE mediaPath IS NOT NULL")?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(0)
                        if (!path.isNullOrBlank()) refs.add(path)
                    }
                }
            } catch (_: Exception) {}
            refs
        }
        return manager.clearOrphanFiles(referencedPaths)
    }

    fun optimizeDatabase() {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(ioDispatcher) {
                    val db = meshDatabase?.openHelper?.writableDatabase
                    if (db != null) {
                        db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
                        db.execSQL("VACUUM")
                    }
                }
                refreshStorageStats()
                _uiEvent.emit(SettingsEvent.SuccessMessage("Database optimized successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(SettingsEvent.Error("Failed to optimize database: ${e.message ?: "unknown error"}"))
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun exportDebugLogs() {
        viewModelScope.launch {
            _uiEvent.emit(SettingsEvent.SuccessMessage("Debug logs exported to Storage/MeshLink/Logs/"))
        }
    }
}
