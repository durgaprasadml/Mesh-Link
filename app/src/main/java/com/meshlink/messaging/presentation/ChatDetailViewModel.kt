package com.meshlink.messaging.presentation

import android.net.Uri
import com.meshlink.common.logger.MeshLogger
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.media.data.VoicePlayer
import com.meshlink.media.data.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectionState {
    DIRECT, RELAY, OFFLINE
}

data class ChatDetailUiState(
    val messages: List<Message> = emptyList(),
    val connectionStatus: ConnectionState = ConnectionState.OFFLINE,
    val transferProgress: Map<String, Float> = emptyMap(),
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val currentlyPlaying: String? = null,
    val playbackProgress: Float = 0f,
    val selectedMessageIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meshRepository: MeshRepository,
    private val getChatMessagesUseCase: com.meshlink.domain.usecase.messaging.GetChatMessagesUseCase,
    private val deleteMessagesUseCase: com.meshlink.domain.usecase.messaging.DeleteMessagesUseCase,
    private val deleteChatUseCase: com.meshlink.domain.usecase.messaging.DeleteChatUseCase,
    private val markChatAsReadUseCase: com.meshlink.domain.usecase.messaging.MarkChatAsReadUseCase,
    private val getMessageUseCase: com.meshlink.domain.usecase.messaging.GetMessageUseCase,
    private val voiceRecorder: VoiceRecorder,
    val voicePlayer: VoicePlayer,
    private val sendMessageUseCase: com.meshlink.domain.usecase.messaging.SendMessageUseCase
) : ViewModel() {

    // URL-decode to recover original strings (colons, spaces, emojis, etc.)
    private val rawPeerIdOrAddress: String = try {
        URLDecoder.decode(savedStateHandle.get<String>("address") ?: "", "UTF-8")
    } catch (_: Exception) {
        savedStateHandle.get<String>("address") ?: ""
    }

    val address: String = meshRepository.resolveChatId(rawPeerIdOrAddress)

    val name: String = try {
        URLDecoder.decode(savedStateHandle.get<String>("name") ?: "Unknown", "UTF-8")
    } catch (_: Exception) {
        savedStateHandle.get<String>("name") ?: "Unknown"
    }

    val messages: StateFlow<List<Message>> = if (address.isNotBlank()) {
        getChatMessagesUseCase(address)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val connectionStatus: StateFlow<ConnectionState> = meshRepository.scannedDevices
        .map { devices ->
            // Check direct scan results
            val isDirect = devices.values.any { it.meshId == rawPeerIdOrAddress || it.address == rawPeerIdOrAddress }
            if (isDirect) return@map ConnectionState.DIRECT

            // Check active BLE connections (GATT clients and connected servers)
            val gattConnected = meshRepository.meshRouter.routeTable.containsKey(rawPeerIdOrAddress) ||
                               meshRepository.meshRouter.routeTable.containsKey(address)
            val hasLiveConnection = meshRepository.isAnyPeerConnected()

            if (gattConnected || hasLiveConnection) {
                ConnectionState.RELAY
            } else {
                ConnectionState.OFFLINE
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.OFFLINE)

    val transferProgress = meshRepository.transferProgress

    val isRecording = voiceRecorder.isRecording
    val recordingElapsedMs = voiceRecorder.elapsedMs

    val currentlyPlaying = voicePlayer.currentlyPlaying
    val playbackProgress = voicePlayer.progress

    // ────────── Selection Logic ──────────

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds = _selectedMessageIds.asStateFlow()

    val uiState: StateFlow<ChatDetailUiState> = combine(
        combine(messages, connectionStatus, transferProgress) { msgs, conn, transfer ->
            Triple(msgs, conn, transfer)
        },
        combine(isRecording, recordingElapsedMs, currentlyPlaying, playbackProgress) { isRec, recMs, playing, prog ->
            listOf(isRec, recMs, playing, prog)
        },
        _selectedMessageIds
    ) { (msgs, conn, transfer), mediaState, selectedIds ->
        val (isRec, recMs, playing, prog) = mediaState
        ChatDetailUiState(
            messages = msgs,
            connectionStatus = conn,
            transferProgress = transfer,
            isRecording = isRec as Boolean,
            recordingElapsedMs = recMs as Long,
            currentlyPlaying = playing as String?,
            playbackProgress = prog as Float,
            selectedMessageIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDetailUiState())

    fun toggleMessageSelection(messageId: String) {
        _selectedMessageIds.update { current ->
            if (current.contains(messageId)) {
                current - messageId
            } else {
                current + messageId
            }
        }
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun deleteSelectedMessages() {
        val idsToDelete = _selectedMessageIds.value
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            deleteMessagesUseCase(idsToDelete.toList())
            clearSelection()
        }
    }

    fun deleteChat() {
        viewModelScope.launch {
            deleteChatUseCase(address)
        }
    }

    init {
        if (address.isNotBlank()) {
            viewModelScope.launch {
                try {
                    meshRepository.autoStartMesh()
                } catch (e: Exception) {
                    MeshLogger.w("ChatDetailVM", "autoStartMesh failed: ${e.message}")
                }
                markChatAsRead()
                try {
                    meshRepository.connectToPeer(rawPeerIdOrAddress.ifBlank { address })
                } catch (e: Exception) {
                    MeshLogger.w("ChatDetailVM", "connectToPeer failed: ${e.message}")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || address.isBlank()) return
        viewModelScope.launch {
            sendMessageUseCase(
                targetMeshId = rawPeerIdOrAddress.ifBlank { address },
                messageText = trimmed,
                chatName = name
            )
        }
    }

    fun markChatAsRead() {
        if (address.isBlank()) return
        viewModelScope.launch {
            try {
                markChatAsReadUseCase(address)
                meshRepository.sendReadReceipts(address)
            } catch (e: Exception) {
                MeshLogger.w("ChatDetailVM", "markChatAsRead failed: ${e.message}")
            }
        }
    }

    fun sendImage(uri: Uri) {
        if (address.isBlank()) return
        viewModelScope.launch {
            meshRepository.sendImage(rawPeerIdOrAddress.ifBlank { address }, uri, name)
        }
    }

    fun retryTransfer(messageId: String) {
        viewModelScope.launch {
            val msg = getMessageUseCase(messageId) ?: return@launch
            if (msg.status == com.meshlink.domain.model.DeliveryStatus.FAILED && msg.mediaPath != null) {
                // Resume upload from the beginning (manual retry)
                val uri = Uri.parse(msg.mediaPath)
                if (msg.messageType == com.meshlink.domain.model.MessageType.IMAGE) {
                    meshRepository.sendImage(rawPeerIdOrAddress.ifBlank { address }, uri, name)
                } else if (msg.messageType == com.meshlink.domain.model.MessageType.VOICE) {
                    meshRepository.sendVoiceNote(rawPeerIdOrAddress.ifBlank { address }, msg.mediaPath, msg.mediaDurationMs ?: 0L, name)
                }
            }
        }
    }

    fun startRecording() {
        voiceRecorder.startRecording()
    }

    fun stopRecordingAndSend() {
        val result = voiceRecorder.stopRecording() ?: return
        val (filePath, durationMs) = result
        if (address.isBlank()) return
        viewModelScope.launch {
            meshRepository.sendVoiceNote(rawPeerIdOrAddress.ifBlank { address }, filePath, durationMs, name)
        }
    }

    fun cancelRecording() {
        voiceRecorder.cancelRecording()
    }

    fun playVoice(filePath: String) {
        voicePlayer.play(filePath)
    }

    fun stopPlayback() {
        voicePlayer.stop()
    }

    fun sendLocation() {
        if (address.isBlank()) return
        viewModelScope.launch {
            meshRepository.sendLocation(rawPeerIdOrAddress.ifBlank { address }, name)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.stop()
    }
}
