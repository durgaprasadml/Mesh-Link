package com.meshlink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.database.data.local.ChatDao
import com.meshlink.domain.model.User
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.profile.AvatarAssets

data class HomeUiState(
    val user: User? = null,
    val userIdentity: UserIdentity? = null,
    val nearbyDevices: List<BleDevice> = emptyList(),
    val unreadChatsCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val meshRepository: MeshRepository,
    chatDao: ChatDao
) : ViewModel() {

    val user: StateFlow<User?> = userRepository.localUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userIdentity: StateFlow<UserIdentity?> = userRepository.localIdentity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<HomeUiState> = combine(
        user,
        userIdentity,
        meshRepository.scannedDevices,
        userRepository.peerIdentities,
        chatDao.getAllChats()
    ) { localUser, identity, scannedDevices, peers, chats ->
        val resolvedDevices = scannedDevices.values.map { device ->
            val peerIdentity = peers[device.meshId] ?: peers[device.address] ?: (if (identity?.userId == device.meshId) identity else null)
            if (peerIdentity != null) {
                val avatarUriStr = when {
                    peerIdentity.galleryImageUri != null -> peerIdentity.galleryImageUri
                    peerIdentity.cameraImageUri != null -> peerIdentity.cameraImageUri
                    peerIdentity.selectedAvatarId != null -> AvatarAssets.buildAvatarUri(peerIdentity.selectedAvatarId)
                    else -> device.avatarUri
                }
                device.copy(
                    name = if (device.name.isBlank() || device.name == "Nearby Node") peerIdentity.displayName else device.name,
                    avatarUri = avatarUriStr
                )
            } else {
                device
            }
        }.sortedByDescending { it.rssi }

        HomeUiState(
            user = localUser,
            userIdentity = identity,
            nearbyDevices = resolvedDevices,
            unreadChatsCount = chats.sumOf { it.unreadCount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun updateUserName(name: String) {
        viewModelScope.launch {
            userRepository.updateUserName(name)
        }
    }
}
