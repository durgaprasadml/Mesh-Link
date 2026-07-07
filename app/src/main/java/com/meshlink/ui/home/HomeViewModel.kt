package com.meshlink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.data.local.ChatDao
import com.meshlink.data.local.UserEntity
import com.meshlink.data.repository.BleRepository
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val bleRepository: BleRepository,
    chatDao: ChatDao
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    val nearbyDevices: StateFlow<List<BleDevice>> = bleRepository.scannedDevices
        .map { it.values.toList().sortedByDescending { device -> device.rssi } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadChatsCount: StateFlow<Int> = chatDao.getAllChats()
        .map { chats -> chats.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadUser()
        autoStartMesh()
    }

    private fun autoStartMesh() {
        viewModelScope.launch {
            try {
                bleRepository.autoStartMesh()
            } catch (_: Exception) { }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            _user.value = userRepository.getLocalUser()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            userRepository.logout()
            onLoggedOut()
        }
    }
}
