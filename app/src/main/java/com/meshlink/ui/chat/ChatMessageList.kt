package com.meshlink.ui.chat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.domain.model.Message

/**
 * Conversation list wrapper delegating to modern Material 3 [MessageList].
 */
@Composable
fun ChatMessageList(
    messages: List<Message>,
    listState: LazyListState,
    selectionState: SelectionState,
    currentlyPlayingVoiceId: String?,
    playbackProgress: Float,
    transferProgressMap: Map<String, Float>,
    paddingValues: PaddingValues,
    onToggleSelection: (String) -> Unit,
    onPlayVoice: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onImageClick: (String) -> Unit,
    onLocationClick: (Double, Double) -> Unit,
    onRetryMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    MessageList(
        messages = messages,
        listState = listState,
        selectionState = selectionState,
        currentlyPlayingVoiceId = currentlyPlayingVoiceId,
        playbackProgress = playbackProgress,
        transferProgressMap = transferProgressMap,
        paddingValues = paddingValues,
        onToggleSelection = onToggleSelection,
        onPlayVoice = onPlayVoice,
        onStopPlayback = onStopPlayback,
        onImageClick = onImageClick,
        onLocationClick = onLocationClick,
        onRetryMedia = onRetryMedia,
        modifier = modifier
    )
}
