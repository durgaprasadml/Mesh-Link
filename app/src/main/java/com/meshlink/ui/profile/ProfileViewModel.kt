package com.meshlink.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            userRepository.localUser.collectLatest { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun saveProfile(name: String, aboutMe: String?, avatarUri: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                userRepository.updateProfile(name, aboutMe, avatarUri)
                _uiState.update { it.copy(isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Failed to save profile") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
