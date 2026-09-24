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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val messages: List<BroadcastUiMessage> = emptyList(),
    val nearbyDevicesCount: Int = 0,
    val isSending: Boolean = false
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository,
    private val getBroadcastMessagesUseCase: GetBroadcastMessagesUseCase
) : ViewModel() {

    private val _isSending = MutableStateFlow(false)

    fun sendBroadcast(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                meshRepository.broadcastMessage(trimmed)
            } finally {
                _isSending.value = false
            }
        }
    }

    val uiState: StateFlow<BroadcastUiState> =
        combine(
            getBroadcastMessagesUseCase(),
            userRepository.observeAllUsers(),
            meshRepository.scannedDevices,
            _isSending
        ) { messages, _, scannedDevices, isSending ->
            // Batch user profile lookups for distinct senders to avoid N+1 queries
            val uniqueSenderIds = messages.map { it.senderId }.distinct()
            val profileCache = uniqueSenderIds.associateWith { senderId ->
                val userProfile = userRepository.getUserProfile(senderId)
                val rawResolvedName = userRepository.getUserDisplayName(senderId)
                val resolvedName = if (!com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(rawResolvedName, senderId)) {
                    rawResolvedName
                } else {
                    "Mesh Peer"
                }
                Pair(resolvedName, userProfile?.profilePhotoPath)
            }

            val uiMessages = messages.map { msg ->
                val (resolvedName, photoPath) = profileCache[msg.senderId] ?: Pair("Mesh Peer", null)
                val cleanText = if (msg.text.startsWith("[BROADCAST]")) {
                    msg.text.removePrefix("[BROADCAST]").trim()
                } else {
                    msg.text
                }
                BroadcastUiMessage(
                    message = msg.copy(text = cleanText),
                    senderName = resolvedName,
                    senderProfilePhotoPath = photoPath
                )
            }
            BroadcastUiState(
                messages = uiMessages,
                nearbyDevicesCount = scannedDevices.size,
                isSending = isSending
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BroadcastUiState())
}

