package com.meshlink.ui.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.data.local.ChatDao
import com.meshlink.data.local.MessageEntity
import com.meshlink.data.media.VoicePlayer
import com.meshlink.data.media.VoiceRecorder
import com.meshlink.data.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bleRepository: BleRepository,
    private val chatDao: ChatDao,
    private val voiceRecorder: VoiceRecorder,
    val voicePlayer: VoicePlayer
) : ViewModel() {

    // URL-decode to recover original strings (colons, spaces, emojis, etc.)
    private val rawPeerIdOrAddress: String = try {
        URLDecoder.decode(savedStateHandle.get<String>("address") ?: "", "UTF-8")
    } catch (_: Exception) {
        savedStateHandle.get<String>("address") ?: ""
    }

    val address: String = bleRepository.resolveChatId(rawPeerIdOrAddress)

    val name: String = try {
        URLDecoder.decode(savedStateHandle.get<String>("name") ?: "Unknown", "UTF-8")
    } catch (_: Exception) {
        savedStateHandle.get<String>("name") ?: "Unknown"
    }

    val messages: StateFlow<List<MessageEntity>> = if (address.isNotBlank()) {
        chatDao.getMessagesForChat(address)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    // FIX Issue 3: Connection status must check actual BLE connections,
    // not just scannedDevices (which resets on every scan restart)
    enum class ConnectionState {
        DIRECT, RELAY, OFFLINE
    }

    val connectionStatus: StateFlow<ConnectionState> = bleRepository.scannedDevices
        .map { devices ->
            // Check direct scan results
            val isDirect = devices.values.any { it.meshId == rawPeerIdOrAddress || it.address == rawPeerIdOrAddress }
            if (isDirect) return@map ConnectionState.DIRECT

            // Check active BLE connections (GATT clients and connected servers)
            val gattConnected = bleRepository.meshRouter.routeTable.containsKey(rawPeerIdOrAddress) ||
                               bleRepository.meshRouter.routeTable.containsKey(address)
            val hasLiveConnection = bleRepository.isAnyPeerConnected()

            if (gattConnected || hasLiveConnection) {
                ConnectionState.RELAY
            } else {
                ConnectionState.OFFLINE
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.OFFLINE)

    val transferProgress = bleRepository.transferProgress

    val isRecording = voiceRecorder.isRecording
    val recordingElapsedMs = voiceRecorder.elapsedMs

    val currentlyPlaying = voicePlayer.currentlyPlaying
    val playbackProgress = voicePlayer.progress

    // ────────── Selection Logic ──────────

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds = _selectedMessageIds.asStateFlow()

    val isSelectionMode = _selectedMessageIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
            chatDao.deleteMessages(idsToDelete.toList())
            clearSelection()
        }
    }

    fun deleteChat() {
        viewModelScope.launch {
            chatDao.deleteChat(address)
        }
    }

    init {
        if (address.isNotBlank()) {
            viewModelScope.launch {
                try {
                    bleRepository.autoStartMesh()
                } catch (e: Exception) {
                    Log.w("ChatDetailVM", "autoStartMesh failed: ${e.message}")
                }
                markChatAsRead()
                try {
                    bleRepository.connectToPeer(rawPeerIdOrAddress.ifBlank { address })
                } catch (e: Exception) {
                    Log.w("ChatDetailVM", "connectToPeer failed: ${e.message}")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || address.isBlank()) return
        viewModelScope.launch {
            bleRepository.sendMessage(rawPeerIdOrAddress.ifBlank { address }, trimmed, name)
        }
    }

    fun markChatAsRead() {
        if (address.isBlank()) return
        viewModelScope.launch {
            try {
                chatDao.markChatAsRead(address)
                bleRepository.sendReadReceipts(address)
            } catch (e: Exception) {
                Log.w("ChatDetailVM", "markChatAsRead failed: ${e.message}")
            }
        }
    }

    fun sendImage(uri: Uri) {
        if (address.isBlank()) return
        viewModelScope.launch {
            bleRepository.sendImage(rawPeerIdOrAddress.ifBlank { address }, uri, name)
        }
    }

    fun retryTransfer(messageId: String) {
        viewModelScope.launch {
            val msg = chatDao.getMessageByUuid(messageId) ?: return@launch
            if (msg.status == com.meshlink.data.local.DeliveryStatus.FAILED && msg.mediaPath != null) {
                // Resume upload from the beginning (manual retry)
                val uri = Uri.parse(msg.mediaPath)
                if (msg.messageType == com.meshlink.data.local.MessageType.IMAGE) {
                    bleRepository.sendImage(rawPeerIdOrAddress.ifBlank { address }, uri, name)
                } else if (msg.messageType == com.meshlink.data.local.MessageType.VOICE) {
                    bleRepository.sendVoiceNote(rawPeerIdOrAddress.ifBlank { address }, msg.mediaPath, msg.mediaDurationMs ?: 0L, name)
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
            bleRepository.sendVoiceNote(rawPeerIdOrAddress.ifBlank { address }, filePath, durationMs, name)
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
            bleRepository.sendLocation(rawPeerIdOrAddress.ifBlank { address }, name)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.stop()
    }
}
