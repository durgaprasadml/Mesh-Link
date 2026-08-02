package com.meshlink.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-quality conversation list composable.
 * Uses reverse layout LazyColumn for modern messaging behavior (bottom-aligned scrolling),
 * sticky date headers, stable key item rendering, and scroll-to-bottom FAB overlay.
 */
@Composable
fun MessageList(
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
    val coroutineScope = rememberCoroutineScope()

    // Determine if scroll-to-bottom FAB should be visible
    val showScrollToBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    // Prepare list reversed for reverse layout rendering (newest message at index 0)
    val reversedMessages = remember(messages) { messages.reversed() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (messages.isEmpty()) {
            EmptyConversation(onSendMessageClick = { })
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = reversedMessages,
                    key = { _, msg -> msg.messageId }
                ) { index, msg ->
                    val isSelected = selectionState.selectedIds.contains(msg.messageId)
                    val progress = transferProgressMap[msg.messageId]

                    // Calculate date header logic
                    val currentDateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(msg.timestamp))
                    val previousMsg = reversedMessages.getOrNull(index + 1)
                    val previousDateStr = previousMsg?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it.timestamp)) }
                    val showDateHeader = previousDateStr == null || currentDateStr != previousDateStr

                    // Calculate message grouping position (SINGLE, FIRST, MIDDLE, LAST)
                    val nextMsg = reversedMessages.getOrNull(index - 1)
                    val isSameAsPrev = previousMsg?.isFromMe == msg.isFromMe && previousDateStr == currentDateStr
                    val isSameAsNext = nextMsg?.isFromMe == msg.isFromMe

                    val position = when {
                        !isSameAsPrev && !isSameAsNext -> BubblePosition.SINGLE
                        !isSameAsPrev && isSameAsNext -> BubblePosition.FIRST
                        isSameAsPrev && isSameAsNext -> BubblePosition.MIDDLE
                        else -> BubblePosition.LAST
                    }

                    Column {
                        MessageBubble(
                            message = msg,
                            position = position,
                            isSelected = isSelected,
                            isSelectionMode = selectionState.isSelectionMode,
                            currentlyPlaying = currentlyPlayingVoiceId,
                            playbackProgress = playbackProgress,
                            transferProgress = progress,
                            onToggleSelection = { onToggleSelection(msg.messageId) },
                            onPlayVoice = onPlayVoice,
                            onStopPlayback = onStopPlayback,
                            onImageClick = onImageClick,
                            onLocationClick = onLocationClick,
                            onRetryMedia = onRetryMedia
                        )

                        if (showDateHeader) {
                            ChatDateHeader(dateText = formatDateLabel(msg.timestamp))
                        }
                    }
                }
            }
        }

        // Scroll to Bottom FAB
        ScrollToBottomButton(
            visible = showScrollToBottom,
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

private fun formatDateLabel(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val fmtDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return when (fmtDate.format(date)) {
        fmtDate.format(now) -> "Today"
        fmtDate.format(Date(now.time - 86400000L)) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(date)
    }
}
