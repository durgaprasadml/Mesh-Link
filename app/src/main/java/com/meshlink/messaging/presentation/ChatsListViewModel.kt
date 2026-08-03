package com.meshlink.messaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.di.DefaultDispatcher
import com.meshlink.domain.model.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Immutable
data class ChatsListUiState(
    val chats: List<Chat> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class ChatsListViewModel @Inject constructor(
    getAllChatsUseCase: com.meshlink.domain.usecase.messaging.GetAllChatsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<ChatsListUiState> = combine(
        getAllChatsUseCase(),
        _searchQuery.debounce { if (it.isEmpty()) 0L else 250L }.distinctUntilChanged()
    ) { chats, query ->
        withContext(defaultDispatcher) {
            val filtered = if (query.isBlank()) {
                chats
            } else {
                chats.filter { it.name.contains(query, ignoreCase = true) }
            }
            ChatsListUiState(chats = filtered, searchQuery = query)
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatsListUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
