package com.meshlink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.database.data.local.ChatDao
import com.meshlink.di.DefaultDispatcher
import com.meshlink.domain.model.User
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val user: User? = null,
    val nearbyDevices: List<BleDevice> = emptyList(),
    val unreadChatsCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val meshRepository: MeshRepository,
    chatDao: ChatDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    val user: StateFlow<User?> = userRepository.localUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.localUser,
        meshRepository.scannedDevices,
        chatDao.getAllChats()
    ) { localUser, scannedDevices, chats ->
        withContext(defaultDispatcher) {
            HomeUiState(
                user = localUser,
                nearbyDevices = scannedDevices.values.toList().sortedByDescending { it.rssi },
                unreadChatsCount = chats.sumOf { it.unreadCount }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun updateUserName(name: String) {
        viewModelScope.launch {
            userRepository.updateUserName(name)
        }
    }
}
