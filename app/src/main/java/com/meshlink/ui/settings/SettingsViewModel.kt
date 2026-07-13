package com.meshlink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: UserEntity? = null,
    val isEncryptionEnabled: Boolean = true,
    val isOnlineVisible: Boolean = true,
    val meshMode: String = "Auto"
)

sealed class SettingsEvent {
    object LogoutSuccess : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<SettingsEvent>(replay = 0)
    val uiEvent = _uiEvent.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.isEncryptionEnabled,
        userRepository.isOnlineVisible,
        userRepository.meshMode
    ) { encryptionEnabled, onlineVisible, meshMode ->
        SettingsUiState(
            user = userRepository.getLocalUser(),
            isEncryptionEnabled = encryptionEnabled,
            isOnlineVisible = onlineVisible,
            meshMode = meshMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userRepository.setEncryptionEnabled(enabled)
        }
    }

    fun setOnlineVisible(visible: Boolean) {
        viewModelScope.launch {
            userRepository.setOnlineVisible(visible)
        }
    }

    fun setMeshMode(mode: String) {
        viewModelScope.launch {
            userRepository.setMeshMode(mode)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _uiEvent.emit(SettingsEvent.LogoutSuccess)
        }
    }
}
