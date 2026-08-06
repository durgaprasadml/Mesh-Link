package com.meshlink.ui.broadcast

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class BroadcastUiMessage(
    val message: Message,
    val senderName: String,
    val senderProfilePhotoPath: String? = null
)

@Immutable
data class BroadcastUiState(
    val messages: List<BroadcastUiMessage> = emptyList()
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository,
    private val getBroadcastMessagesUseCase: GetBroadcastMessagesUseCase
) : ViewModel() {

    fun sendBroadcast(message: String) {
        viewModelScope.launch {
            meshRepository.broadcastMessage(message)
        }
    }

    val uiState: StateFlow<BroadcastUiState> =
        getBroadcastMessagesUseCase()
            .map { messages ->
                val uiMessages = messages.map { msg ->
                    val userProfile = userRepository.getUserProfile(msg.senderId)
                    val resolvedName = userRepository.getUserDisplayName(msg.senderId)
                    val cleanText = if (msg.text.startsWith("[BROADCAST]")) {
                        msg.text.removePrefix("[BROADCAST]").trim()
                    } else {
                        msg.text
                    }
                    BroadcastUiMessage(
                        message = msg.copy(text = cleanText),
                        senderName = resolvedName,
                        senderProfilePhotoPath = userProfile?.profilePhotoPath
                    )
                }
                BroadcastUiState(messages = uiMessages)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BroadcastUiState())
}
