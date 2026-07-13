package com.meshlink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: UserEntity? = null,
    val nearbyDevices: List<BleDevice> = emptyList(),
    val unreadChatsCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val meshRepository: MeshRepository,
    chatDao: ChatDao
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        _user,
        meshRepository.scannedDevices,
        chatDao.getAllChats()
    ) { user, scannedDevices, chats ->
        HomeUiState(
            user = user,
            nearbyDevices = scannedDevices.values.toList().sortedByDescending { it.rssi },
            unreadChatsCount = chats.sumOf { it.unreadCount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        loadUser()
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            // Placeholder: ideally update in UserDao
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getLocalUser()
        }
    }

}
