package com.meshlink.ui.chat

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.UserIdentity
import com.meshlink.messaging.presentation.ChatDetailUiState
import com.meshlink.messaging.presentation.MediaViewerScreen
import com.meshlink.ui.components.MeshScreen

/**
 * Main presentation composable for Mesh-Link Phase 5 Chat Experience Redesign.
 * Composes tactical background, header, search overlay, lazy message list, typing indicators, and floating glass input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerIdentity: UserIdentity,
    peerAddress: String,
    fallbackName: String,
    uiState: ChatDetailUiState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit,
    onSendLocation: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecordingAndSend: () -> Unit,
    onCancelRecording: () -> Unit,
    onToggleMessageSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onDeleteChat: () -> Unit,
    onPlayVoice: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onRetryTransfer: (String) -> Unit,
    onOpenLocation: (Double, Double) -> Unit,
    onLaunchGallery: () -> Unit = {},
    onLaunchCamera: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableStateOf(0) }

    var showAttachmentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fullscreenMessageId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (uiState.messages.isNotEmpty()) uiState.messages.size - 1 else 0
    )

    val clipboardManager = LocalClipboardManager.current

    // Filter messages for search overlay if active
    val displayMessages = remember(uiState.messages, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.messages
        } else {
            uiState.messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Scroll to bottom on new message arrival
    var previousLastMessageId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.messages) {
        val currentLast = uiState.messages.lastOrNull()?.messageId
        if (currentLast != null && currentLast != previousLastMessageId) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
        previousLastMessageId = currentLast
    }

    // Media Viewer Fullscreen Dialog
    if (fullscreenMessageId != null) {
        val mediaMessages = uiState.messages.filter { it.messageType == MessageType.IMAGE }
        val initialIndex = mediaMessages.indexOfFirst { it.messageId == fullscreenMessageId }.coerceAtLeast(0)

        Dialog(
            onDismissRequest = { fullscreenMessageId = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            MediaViewerScreen(
                mediaMessages = mediaMessages,
                initialIndex = initialIndex,
                onBack = { fullscreenMessageId = null },
                onDelete = { msg ->
                    if (!uiState.selectedMessageIds.contains(msg.messageId)) {
                        onToggleMessageSelection(msg.messageId)
                    }
                    onDeleteSelectedMessages()
                    fullscreenMessageId = null
                }
            )
        }
    }

    // Attachment Picker Bottom Sheet
    if (showAttachmentSheet) {
        AttachmentPickerBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAttachmentSheet = false },
            onGalleryClick = {
                showAttachmentSheet = false
                onLaunchGallery()
            },
            onCameraClick = {
                showAttachmentSheet = false
                onLaunchCamera()
            },
            onLocationClick = {
                showAttachmentSheet = false
                onSendLocation()
            }
        )
    }

    ChatBackground(connectionState = uiState.connectionStatus) {
        MeshScreen(
            modifier = modifier,
            topBar = {
                Box {
                    ChatTopBar(
                        peerIdentity = peerIdentity,
                        peerAddress = peerAddress,
                        fallbackName = fallbackName,
                        connectionState = uiState.connectionStatus,
                        selectionState = SelectionState(
                            selectedIds = uiState.selectedMessageIds,
                            isSelectionMode = uiState.isSelectionMode
                        ),
                        onBackClick = onBack,
                        onClearSelection = onClearSelection,
                        onDeleteSelected = onDeleteSelectedMessages,
                        onCopySelected = {
                            val selectedTexts = uiState.messages
                                .filter { uiState.selectedMessageIds.contains(it.messageId) }
                                .joinToString("\n") { it.text }
                            if (selectedTexts.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(selectedTexts))
                            }
                            onClearSelection()
                        },
                        onDeleteChat = onDeleteChat,
                        onSearchClick = { isSearchVisible = !isSearchVisible }
                    )

                    ChatSearchOverlay(
                        isVisible = isSearchVisible,
                        searchQuery = searchQuery,
                        matchCount = displayMessages.size,
                        currentMatchIndex = currentMatchIndex,
                        onQueryChange = {
                            searchQuery = it
                            currentMatchIndex = 0
                        },
                        onNextMatch = {
                            if (displayMessages.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex + 1) % displayMessages.size
                            }
                        },
                        onPreviousMatch = {
                            if (displayMessages.isNotEmpty()) {
                                currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else displayMessages.size - 1
                            }
                        },
                        onCloseSearch = {
                            isSearchVisible = false
                            searchQuery = ""
                        }
                    )
                }
            },
            bottomBar = {
                if (!uiState.isSelectionMode) {
                    ChatInputBar(
                        inputText = inputText,
                        onInputTextChanged = { inputText = it },
                        isRecording = uiState.isRecording,
                        recordingElapsedMs = uiState.recordingElapsedMs,
                        activeReplyState = null,
                        onCancelReply = {},
                        onStartRecording = onStartRecording,
                        onStopRecordingAndSend = onStopRecordingAndSend,
                        onCancelRecording = onCancelRecording,
                        onSendText = { text ->
                            onSendMessage(text)
                            inputText = ""
                        },
                        onAttachClick = { showAttachmentSheet = true }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                ChatMessageList(
                    messages = displayMessages,
                    listState = listState,
                    selectionState = SelectionState(
                        selectedIds = uiState.selectedMessageIds,
                        isSelectionMode = uiState.isSelectionMode
                    ),
                    currentlyPlayingVoiceId = uiState.currentlyPlaying,
                    playbackProgress = uiState.playbackProgress,
                    transferProgressMap = uiState.transferProgress,
                    paddingValues = paddingValues,
                    onToggleSelection = onToggleMessageSelection,
                    onPlayVoice = onPlayVoice,
                    onStopPlayback = onStopPlayback,
                    onImageClick = { id -> if (!uiState.isSelectionMode) fullscreenMessageId = id },
                    onLocationClick = onOpenLocation,
                    onRetryMedia = onRetryTransfer
                )
            }
        }
    }
}
