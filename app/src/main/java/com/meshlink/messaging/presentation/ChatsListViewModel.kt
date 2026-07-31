package com.meshlink.messaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import com.meshlink.domain.repository.UserRepository
import com.meshlink.ui.profile.AvatarAssets
import kotlinx.coroutines.flow.combine

@androidx.compose.runtime.Immutable
data class ChatsListUiState(
    val chats: List<Chat> = emptyList()
)

@HiltViewModel
class ChatsListViewModel @Inject constructor(
    getAllChatsUseCase: com.meshlink.domain.usecase.messaging.GetAllChatsUseCase,
    userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<ChatsListUiState> = combine(
        getAllChatsUseCase(),
        userRepository.peerIdentities,
        userRepository.localIdentity
    ) { chats, peers, local ->
        val updatedChats = chats.map { chat ->
            val peerIdentity = peers[chat.id] ?: (if (local?.userId == chat.id) local else null)
            if (peerIdentity != null) {
                val avatarUriStr = when {
                    peerIdentity.galleryImageUri != null -> peerIdentity.galleryImageUri
                    peerIdentity.cameraImageUri != null -> peerIdentity.cameraImageUri
                    peerIdentity.selectedAvatarId != null -> AvatarAssets.buildAvatarUri(peerIdentity.selectedAvatarId)
                    else -> chat.avatarUri
                }
                chat.copy(
                    name = chat.name.ifBlank { peerIdentity.displayName },
                    avatarUri = avatarUriStr
                )
            } else {
                chat
            }
        }
        ChatsListUiState(chats = updatedChats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatsListUiState())

}

