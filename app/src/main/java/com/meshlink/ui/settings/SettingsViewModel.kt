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
    
    // Network
    val isBleEnabled: Boolean = true,
    val isWifiDirectEnabled: Boolean = true,
    val preferredTransport: String = "HYBRID",
    val isMeshRelayEnabled: Boolean = true,
    
    // Appearance
    val themeMode: String = "SYSTEM",
    val isMaterialYouEnabled: Boolean = true,
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false
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

    val uiState: StateFlow<SettingsUiState> = combine(
        _user,
        combine(
            userRepository.isEncryptionEnabled,
            userRepository.isOnlineVisible,
            userRepository.meshMode,
            settingsRepository.isAppLockEnabled,
            settingsRepository.autoLockTimeoutMs
        ) { enc, onl, mesh, lock, timeout ->
            Triple(Triple(enc, onl, mesh), lock, timeout)
        },
        combine(
            settingsRepository.isBiometricsEnabled,
            settingsRepository.isBleEnabled,
            settingsRepository.isWifiDirectEnabled,
            settingsRepository.preferredTransport,
            settingsRepository.isMeshRelayEnabled
        ) { bio, ble, wifi, trans, relay ->
            Triple(bio, Triple(ble, wifi, trans), relay)
        },
        combine(
            settingsRepository.themeMode,
            settingsRepository.isMaterialYouEnabled,
            settingsRepository.fontScale,
            settingsRepository.highContrast
        ) { theme, mat, font, contrast ->
            Triple(theme, mat, font to contrast)
        }
    ) { user, group1, group2, group3 ->
        val (userSettings, lock, timeout) = group1
        val (enc, onl, mesh) = userSettings
        
        val (bio, netGroup, relay) = group2
        val (ble, wifi, trans) = netGroup
        
        val (theme, mat, fontContrast) = group3
        val (font, contrast) = fontContrast

        SettingsUiState(
            user = user,
            isEncryptionEnabled = enc,
            isOnlineVisible = onl,
            meshMode = mesh,
            isAppLockEnabled = lock,
            autoLockTimeoutMs = timeout,
            isBiometricsEnabled = bio,
            isBleEnabled = ble,
            isWifiDirectEnabled = wifi,
            preferredTransport = trans,
            isMeshRelayEnabled = relay,
            themeMode = theme,
            isMaterialYouEnabled = mat,
            fontScale = font,
            highContrast = contrast
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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
    fun setEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch { userRepository.setEncryptionEnabled(enabled) }
    }

    fun setOnlineVisible(visible: Boolean) {
        viewModelScope.launch { userRepository.setOnlineVisible(visible) }
    }

    fun setMeshMode(mode: String) {
        viewModelScope.launch { userRepository.setMeshMode(mode) }
    }

    // Security Settings
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    }
    
    fun setAutoLockTimeoutMs(timeoutMs: Long) {
        viewModelScope.launch { settingsRepository.setAutoLockTimeoutMs(timeoutMs) }
    }
    
    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricsEnabled(enabled) }
    }
    
    // Network Settings
    fun setBleEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBleEnabled(enabled) }
    }
    
    fun setWifiDirectEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiDirectEnabled(enabled) }
    }
    
    fun setPreferredTransport(transport: String) {
        viewModelScope.launch { settingsRepository.setPreferredTransport(transport) }
    }
    
    fun setMeshRelayEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMeshRelayEnabled(enabled) }
    }

    // Appearance Settings
    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
    
    fun setMaterialYouEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMaterialYouEnabled(enabled) }
    }
    
    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
    }
    
    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHighContrast(enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _uiEvent.emit(SettingsEvent.LogoutSuccess)
        }
    }
}
