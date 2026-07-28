package com.meshlink.ui.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BroadcastUiState(
    val messages: List<Message> = emptyList()
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val getBroadcastMessagesUseCase: GetBroadcastMessagesUseCase
) : ViewModel() {

    fun sendBroadcast(message: String) {
        viewModelScope.launch {
            meshRepository.broadcastMessage(message)
        }
    }

    val uiState: StateFlow<BroadcastUiState> =
        getBroadcastMessagesUseCase()
            .map { BroadcastUiState(messages = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BroadcastUiState())
}
