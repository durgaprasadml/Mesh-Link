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

data class SettingsUiState(
    val user: User? = null,
    
    // Legacy UserRepository settings
    val isEncryptionEnabled: Boolean = true,
    val isOnlineVisible: Boolean = true,
    val meshMode: String = "Auto",
    
    // Security
    val isAppLockEnabled: Boolean = false,
    val autoLockTimeoutMs: Long = 60000L,
    val isBiometricsEnabled: Boolean = false,
    
    // Network - Bluetooth
    val isBleEnabled: Boolean = true,
    val bleAdvertisingEnabled: Boolean = true,
    val bleScanningEnabled: Boolean = true,
    val bleTxPower: Int = 2,
    val bleScanInterval: Long = 5000L,
    val bleAutoRestart: Boolean = true,

    // Network - WiFi Direct
    val isWifiDirectEnabled: Boolean = true,
    val wifiAutoConnect: Boolean = true,
    val wifiPeerDiscoveryEnabled: Boolean = true,
    val wifiPreferredGroupOwner: Boolean = false,
    val wifiReconnectEnabled: Boolean = true,

    // Transport Mode
    val preferredTransport: String = "HYBRID",

    // Relay
    val isMeshRelayEnabled: Boolean = true,
    val meshMaxHops: Int = 5,
    val meshTtl: Int = 10,
    val meshPriority: Int = 1,
    val meshQueueSize: Int = 1000,

    // Discovery
    val discoveryInterval: Long = 30000L,
    val discoveryBackground: Boolean = true,
    val discoveryForeground: Boolean = true,
    val discoveryTimeout: Long = 120000L,
    val discoveryRestart: Boolean = true,

    // Advanced
    val advancedPacketSize: Int = 512,
    val advancedRetryCount: Int = 3,
    val advancedCompression: Boolean = true,
    val advancedEncryptionEnforcement: Boolean = true,
    val advancedBandwidthOptimization: Boolean = true,
    
    // Appearance
    val themeMode: String = "SYSTEM",
    val isMaterialYouEnabled: Boolean = true,
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false,
    val accentColor: String = "Blue",
    val animationsEnabled: Boolean = true,
    val glassEffectsEnabled: Boolean = true,
    val cornerRadiusScale: Float = 1.0f,
    val largeTextEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false
)

sealed class SettingsEvent {
    object LogoutSuccess : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
    data class SuccessMessage(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<SettingsEvent>(replay = 0)
    val uiEvent = _uiEvent.asSharedFlow()

    private val _user = MutableStateFlow<User?>(null)

    init {
        viewModelScope.launch {
            _user.value = userRepository.getLocalUser()
        }
    }

    private val combinedState: Flow<SettingsUiState> = combine(
        _user,
        combine(
            userRepository.isEncryptionEnabled,
            userRepository.isOnlineVisible,
            userRepository.meshMode,
            settingsRepository.isAppLockEnabled,
            settingsRepository.autoLockTimeoutMs
        ) { enc, onl, mesh, lock, timeout ->
            SettingsGroup1(enc, onl, mesh, lock, timeout)
        },
        combine(
            settingsRepository.isBiometricsEnabled,
            settingsRepository.isBleEnabled,
            settingsRepository.bleAdvertisingEnabled,
            settingsRepository.bleScanningEnabled,
            settingsRepository.bleTxPower
        ) { bio, ble, bleAdv, bleScan, bleTx ->
            SettingsGroup2(bio, ble, bleAdv, bleScan, bleTx)
        },
        combine(
            settingsRepository.bleScanInterval,
            settingsRepository.bleAutoRestart,
            settingsRepository.isWifiDirectEnabled,
            settingsRepository.wifiAutoConnect,
            settingsRepository.wifiPeerDiscoveryEnabled
        ) { bleInt, bleAuto, wifi, wifiAuto, wifiDisc ->
            SettingsGroup3(bleInt, bleAuto, wifi, wifiAuto, wifiDisc)
        },
        combine(
            settingsRepository.wifiPreferredGroupOwner,
            settingsRepository.wifiReconnectEnabled,
            settingsRepository.preferredTransport,
            settingsRepository.isMeshRelayEnabled,
            settingsRepository.meshMaxHops
        ) { wifiGo, wifiRec, trans, relay, hops ->
            SettingsGroup4(wifiGo, wifiRec, trans, relay, hops)
        },
        combine(
            settingsRepository.meshTtl,
            settingsRepository.meshPriority,
            settingsRepository.meshQueueSize,
            settingsRepository.discoveryInterval,
            settingsRepository.discoveryBackground
        ) { ttl, prio, queue, dInt, dBack ->
            SettingsGroup5(ttl, prio, queue, dInt, dBack)
        },
        combine(
            settingsRepository.discoveryForeground,
            settingsRepository.discoveryTimeout,
            settingsRepository.discoveryRestart,
            settingsRepository.advancedPacketSize,
            settingsRepository.advancedRetryCount
        ) { dFore, dTime, dRest, pSize, retries ->
            SettingsGroup6(dFore, dTime, dRest, pSize, retries)
        },
        combine(
            settingsRepository.advancedCompression,
            settingsRepository.advancedEncryptionEnforcement,
            settingsRepository.advancedBandwidthOptimization,
            settingsRepository.themeMode,
            settingsRepository.isMaterialYouEnabled
        ) { comp, encEnf, bwOpt, theme, mat ->
            SettingsGroup7(comp, encEnf, bwOpt, theme, mat)
        }
    ) { args ->
        val user = args[0] as User?
        val g1 = args[1] as SettingsGroup1
        val g2 = args[2] as SettingsGroup2
        val g3 = args[3] as SettingsGroup3
        val g4 = args[4] as SettingsGroup4
        val g5 = args[5] as SettingsGroup5
        val g6 = args[6] as SettingsGroup6
        val g7 = args[7] as SettingsGroup7
        
        // Fetch the remaining ones that didn't fit (fontScale, highContrast) in a separate combine
        // Actually it's easier to just do one more combine block:
        SettingsUiState(
            user = user,
            isEncryptionEnabled = g1.enc,
            isOnlineVisible = g1.onl,
            meshMode = g1.mesh,
            isAppLockEnabled = g1.lock,
            autoLockTimeoutMs = g1.timeout,
            
            isBiometricsEnabled = g2.bio,
            isBleEnabled = g2.ble,
            bleAdvertisingEnabled = g2.bleAdv,
            bleScanningEnabled = g2.bleScan,
            bleTxPower = g2.bleTx,
            
            bleScanInterval = g3.bleInt,
            bleAutoRestart = g3.bleAuto,
            isWifiDirectEnabled = g3.wifi,
            wifiAutoConnect = g3.wifiAuto,
            wifiPeerDiscoveryEnabled = g3.wifiDisc,
            
            wifiPreferredGroupOwner = g4.wifiGo,
            wifiReconnectEnabled = g4.wifiRec,
            preferredTransport = g4.trans,
            isMeshRelayEnabled = g4.relay,
            meshMaxHops = g4.hops,
            
            meshTtl = g5.ttl,
            meshPriority = g5.prio,
            meshQueueSize = g5.queue,
            discoveryInterval = g5.dInt,
            discoveryBackground = g5.dBack,
            
            discoveryForeground = g6.dFore,
            discoveryTimeout = g6.dTime,
            discoveryRestart = g6.dRest,
            advancedPacketSize = g6.pSize,
            advancedRetryCount = g6.retries,
            
            advancedCompression = g7.comp,
            advancedEncryptionEnforcement = g7.encEnf,
            advancedBandwidthOptimization = g7.bwOpt,
            themeMode = g7.theme,
            isMaterialYouEnabled = g7.mat
        )
    }

    // Fetch the remaining properties in a separate combine
    val uiState = combine(
        combinedState,
        settingsRepository.fontScale,
        settingsRepository.highContrast,
        settingsRepository.accentColor,
        settingsRepository.animationsEnabled,
        settingsRepository.glassEffectsEnabled,
        settingsRepository.cornerRadiusScale,
        settingsRepository.largeTextEnabled,
        settingsRepository.reduceMotionEnabled
    ) { args ->
        val state = args[0] as SettingsUiState
        state.copy(
            fontScale = args[1] as Float,
            highContrast = args[2] as Boolean,
            accentColor = args[3] as String,
            animationsEnabled = args[4] as Boolean,
            glassEffectsEnabled = args[5] as Boolean,
            cornerRadiusScale = args[6] as Float,
            largeTextEnabled = args[7] as Boolean,
            reduceMotionEnabled = args[8] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // Internal data classes for grouping
    private data class SettingsGroup1(val enc: Boolean, val onl: Boolean, val mesh: String, val lock: Boolean, val timeout: Long)
    private data class SettingsGroup2(val bio: Boolean, val ble: Boolean, val bleAdv: Boolean, val bleScan: Boolean, val bleTx: Int)
    private data class SettingsGroup3(val bleInt: Long, val bleAuto: Boolean, val wifi: Boolean, val wifiAuto: Boolean, val wifiDisc: Boolean)
    private data class SettingsGroup4(val wifiGo: Boolean, val wifiRec: Boolean, val trans: String, val relay: Boolean, val hops: Int)
    private data class SettingsGroup5(val ttl: Int, val prio: Int, val queue: Int, val dInt: Long, val dBack: Boolean)
    private data class SettingsGroup6(val dFore: Boolean, val dTime: Long, val dRest: Boolean, val pSize: Int, val retries: Int)
    private data class SettingsGroup7(val comp: Boolean, val encEnf: Boolean, val bwOpt: Boolean, val theme: String, val mat: Boolean)

    // Profile Settings
    fun updateUserName(name: String) {
        viewModelScope.launch {
            try {
                userRepository.updateUserName(name)
                _user.value = userRepository.getLocalUser()
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

    // Security Settings
    fun setAppLockEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    fun setAutoLockTimeoutMs(timeoutMs: Long) = viewModelScope.launch { settingsRepository.setAutoLockTimeoutMs(timeoutMs) }
    fun setBiometricsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBiometricsEnabled(enabled) }
    
    // Bluetooth Settings
    fun setBleEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleEnabled(enabled) }
    fun setBleAdvertisingEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleAdvertisingEnabled(enabled) }
    fun setBleScanningEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleScanningEnabled(enabled) }
    fun setBleTxPower(power: Int) = viewModelScope.launch { settingsRepository.setBleTxPower(power) }
    fun setBleScanInterval(interval: Long) = viewModelScope.launch { settingsRepository.setBleScanInterval(interval) }
    fun setBleAutoRestart(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBleAutoRestart(enabled) }

    // WiFi Direct Settings
    fun setWifiDirectEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWifiDirectEnabled(enabled) }
    fun setWifiAutoConnect(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWifiAutoConnect(enabled) }
    fun setWifiPeerDiscoveryEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWifiPeerDiscoveryEnabled(enabled) }
    fun setWifiPreferredGroupOwner(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWifiPreferredGroupOwner(enabled) }
    fun setWifiReconnectEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWifiReconnectEnabled(enabled) }

    // Transport Mode
    fun setPreferredTransport(transport: String) = viewModelScope.launch { settingsRepository.setPreferredTransport(transport) }

    // Relay
    fun setMeshRelayEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setMeshRelayEnabled(enabled) }
    fun setMeshMaxHops(hops: Int) = viewModelScope.launch { settingsRepository.setMeshMaxHops(hops) }
    fun setMeshTtl(ttl: Int) = viewModelScope.launch { settingsRepository.setMeshTtl(ttl) }
    fun setMeshPriority(priority: Int) = viewModelScope.launch { settingsRepository.setMeshPriority(priority) }
    fun setMeshQueueSize(size: Int) = viewModelScope.launch { settingsRepository.setMeshQueueSize(size) }

    // Discovery
    fun setDiscoveryInterval(interval: Long) = viewModelScope.launch { settingsRepository.setDiscoveryInterval(interval) }
    fun setDiscoveryBackground(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDiscoveryBackground(enabled) }
    fun setDiscoveryForeground(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDiscoveryForeground(enabled) }
    fun setDiscoveryTimeout(timeout: Long) = viewModelScope.launch { settingsRepository.setDiscoveryTimeout(timeout) }
    fun setDiscoveryRestart(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDiscoveryRestart(enabled) }

    // Advanced
    fun setAdvancedPacketSize(size: Int) = viewModelScope.launch { settingsRepository.setAdvancedPacketSize(size) }
    fun setAdvancedRetryCount(count: Int) = viewModelScope.launch { settingsRepository.setAdvancedRetryCount(count) }
    fun setAdvancedCompression(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAdvancedCompression(enabled) }
    fun setAdvancedEncryptionEnforcement(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAdvancedEncryptionEnforcement(enabled) }
    fun setAdvancedBandwidthOptimization(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAdvancedBandwidthOptimization(enabled) }

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

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _uiEvent.emit(SettingsEvent.LogoutSuccess)
        }
    }
}
