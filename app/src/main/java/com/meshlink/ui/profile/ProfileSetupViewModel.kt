package com.meshlink.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProfileSetupUiState {
    object Idle : ProfileSetupUiState()
    object Loading : ProfileSetupUiState()
}

sealed class ProfileSetupEvent {
    object SetupSuccess : ProfileSetupEvent()
    data class Error(val message: String) : ProfileSetupEvent()
}

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val hasProfile: StateFlow<Boolean?> = userRepository.hasProfile
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Idle)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProfileSetupEvent>(replay = 0)
    val uiEvent = _uiEvent.asSharedFlow()

    fun resetState() {
        _uiState.value = ProfileSetupUiState.Idle
    }

    fun createProfile(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.length < 2 || trimmedName.length > 30) {
            viewModelScope.launch { _uiEvent.emit(ProfileSetupEvent.Error("Display name must be between 2 and 30 characters")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState.Loading
            val result = userRepository.createProfile(trimmedName)
            if (result.isSuccess) {
                _uiState.value = ProfileSetupUiState.Idle
                _uiEvent.emit(ProfileSetupEvent.SetupSuccess)
            } else {
                _uiState.value = ProfileSetupUiState.Idle
                _uiEvent.emit(ProfileSetupEvent.Error(result.exceptionOrNull()?.message ?: "Failed to create profile"))
            }
        }
    }
}
