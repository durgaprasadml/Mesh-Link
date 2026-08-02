package com.meshlink.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.Message
import com.meshlink.ui.components.chat.DateSeparator
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.util.Calendar

/**
 * Optimized LazyColumn Message List composable with date headers, stable key tracking,
 * and adaptive message grouping position computation.
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
    onSwipeToReply: (Message) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = messages,
            key = { _, msg -> msg.messageId },
            contentType = { _, msg -> msg.messageType.name }
        ) { index, msg ->
            // Date Separator logic
            val showDateSeparator = shouldShowDateSeparator(
                currentTimestamp = msg.timestamp,
                previousTimestamp = if (index > 0) messages[index - 1].timestamp else null
            )

            if (showDateSeparator) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DateSeparator(timestamp = msg.timestamp)
                }
            }

            // Adaptive Bubble Position computation based on adjacent senders
            val prevMsg = if (index > 0) messages[index - 1] else null
            val nextMsg = if (index < messages.size - 1) messages[index + 1] else null

            val isSameSenderAsPrev = prevMsg != null && prevMsg.isFromMe == msg.isFromMe && !showDateSeparator && (msg.timestamp - prevMsg.timestamp < 300_000)
            val isSameSenderAsNext = nextMsg != null && nextMsg.isFromMe == msg.isFromMe && (nextMsg.timestamp - msg.timestamp < 300_000)

            val bubblePosition = when {
                !isSameSenderAsPrev && !isSameSenderAsNext -> BubblePosition.SINGLE
                !isSameSenderAsPrev && isSameSenderAsNext -> BubblePosition.FIRST
                isSameSenderAsPrev && isSameSenderAsNext -> BubblePosition.MIDDLE
                else -> BubblePosition.LAST
            }

            val isSelected = selectionState.selectedIds.contains(msg.messageId)
            val msgTransferProgress = transferProgressMap[msg.messageId]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                MessageBubble(
                    message = msg,
                    position = bubblePosition,
                    isSelected = isSelected,
                    isSelectionMode = selectionState.isSelectionMode,
                    currentlyPlaying = currentlyPlayingVoiceId,
                    playbackProgress = playbackProgress,
                    transferProgress = msgTransferProgress,
                    onToggleSelection = { onToggleSelection(msg.messageId) },
                    onPlayVoice = onPlayVoice,
                    onStopPlayback = onStopPlayback,
                    onImageClick = onImageClick,
                    onLocationClick = onLocationClick,
                    onRetryMedia = onRetryMedia,
                    onSwipeToReply = onSwipeToReply
                )
            }
        }
    }
}

private fun shouldShowDateSeparator(currentTimestamp: Long, previousTimestamp: Long?): Boolean {
    if (previousTimestamp == null) return true

    val currentCal = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    val previousCal = Calendar.getInstance().apply { timeInMillis = previousTimestamp }

    return currentCal.get(Calendar.YEAR) != previousCal.get(Calendar.YEAR) ||
            currentCal.get(Calendar.DAY_OF_YEAR) != previousCal.get(Calendar.DAY_OF_YEAR)
}
