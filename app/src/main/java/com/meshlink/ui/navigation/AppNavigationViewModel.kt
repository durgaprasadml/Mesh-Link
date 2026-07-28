package com.meshlink.ui.navigation

import androidx.lifecycle.ViewModel
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {
    val hasProfile: Flow<Boolean> = userRepository.hasProfile
}
