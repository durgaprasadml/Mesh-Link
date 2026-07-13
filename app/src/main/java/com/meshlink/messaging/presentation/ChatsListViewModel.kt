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

data class ChatsListUiState(
    val chats: List<Chat> = emptyList()
)

@HiltViewModel
class ChatsListViewModel @Inject constructor(
    getAllChatsUseCase: com.meshlink.domain.usecase.messaging.GetAllChatsUseCase
) : ViewModel() {

    val uiState: StateFlow<ChatsListUiState> = getAllChatsUseCase()
        .map { ChatsListUiState(chats = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatsListUiState())

}
