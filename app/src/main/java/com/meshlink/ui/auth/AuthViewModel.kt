package com.meshlink.ui.auth

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
}

sealed class AuthEvent {
    object LoginSuccess : AuthEvent()
    object RegistrationSuccess : AuthEvent()
    data class Error(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val isUserLoggedIn = userRepository.isUserLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AuthEvent>(replay = 0)
    val uiEvent = _uiEvent.asSharedFlow()

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun register(name: String, phoneNumber: String, pin: String) {
        if (pin.length != 4) {
            viewModelScope.launch { _uiEvent.emit(AuthEvent.Error("PIN must be 4 digits")) }
            return
        }
        if (name.isBlank() || phoneNumber.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(AuthEvent.Error("Fields cannot be empty")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.registerUser(name, phoneNumber, pin).fold(
                onSuccess = { meshId ->
                    _uiState.value = AuthUiState.Idle
                    _uiEvent.emit(AuthEvent.RegistrationSuccess)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Idle
                    _uiEvent.emit(AuthEvent.Error(error.message ?: "Unknown error"))
                }
            )
        }
    }

    fun login(phoneNumber: String, pin: String) {
        if (phoneNumber.isBlank() || pin.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(AuthEvent.Error("Fields cannot be empty")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.loginUser(phoneNumber, pin).fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Idle
                    _uiEvent.emit(AuthEvent.LoginSuccess)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Idle
                    _uiEvent.emit(AuthEvent.Error(error.message ?: "Login failed"))
                }
            )
        }
    }
}
