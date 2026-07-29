package com.meshlink.ui.landing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LandingUiState(
    val isWelcomeMode: Boolean = false,
    val userName: String = "",
    val avatarUri: String? = null,
    val isCompleted: Boolean = false
)

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    init {
        val isWelcomeArg = savedStateHandle.get<Boolean>("isWelcome") ?: false
        _uiState.value = _uiState.value.copy(isWelcomeMode = isWelcomeArg)

        viewModelScope.launch {
            userRepository.localUser.collect { user ->
                user?.let {
                    _uiState.value = _uiState.value.copy(
                        userName = it.name,
                        avatarUri = it.avatarUri
                    )
                }
            }
        }
    }

    fun onSkipClicked() {
        _uiState.value = _uiState.value.copy(isCompleted = true)
    }
}
