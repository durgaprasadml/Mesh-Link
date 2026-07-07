package com.meshlink.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val isUserLoggedIn = userRepository.isUserLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun register(name: String, phoneNumber: String, pin: String) {
        if (pin.length != 4) {
            _authState.value = AuthState.Error("PIN must be 4 digits")
            return
        }
        if (name.isBlank() || phoneNumber.isBlank()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userRepository.registerUser(name, phoneNumber, pin).fold(
                onSuccess = { meshId ->
                    _authState.value = AuthState.Success
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun login(phoneNumber: String, pin: String) {
        if (phoneNumber.isBlank() || pin.isBlank()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userRepository.loginUser(phoneNumber, pin).fold(
                onSuccess = {
                    _authState.value = AuthState.Success
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
