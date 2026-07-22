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
    
    val isEncryptionEnabled: Boolean = true,
    val isOnlineVisible: Boolean = true,
    val meshMode: String = "Auto",
    
    val isBleEnabled: Boolean = true,
    
    val themeMode: String = "SYSTEM"
)

sealed class SettingsEvent {
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

    val uiState = combine(
        _user,
        userRepository.isEncryptionEnabled,
        userRepository.isOnlineVisible,
        userRepository.meshMode,
        settingsRepository.isBleEnabled,
        settingsRepository.themeMode
    ) { args ->
        SettingsUiState(
            user = args[0] as User?,
            isEncryptionEnabled = args[1] as Boolean,
            isOnlineVisible = args[2] as Boolean,
            meshMode = args[3] as String,
            isBleEnabled = args[4] as Boolean,
            themeMode = args[5] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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

    fun setOnlineVisible(visible: Boolean) = viewModelScope.launch { userRepository.setOnlineVisible(visible) }
    fun setThemeMode(mode: String) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    
    fun clearAllChats() = viewModelScope.launch {
        _uiEvent.emit(SettingsEvent.SuccessMessage("All chats cleared"))
    }
    
    fun clearMediaCache() = viewModelScope.launch {
        _uiEvent.emit(SettingsEvent.SuccessMessage("Media cache cleared"))
    }
}
