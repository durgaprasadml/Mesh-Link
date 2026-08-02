package com.meshlink.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Presentation input bar wrapper delegating to modern Material 3 [MessageComposer].
 */
@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    isRecording: Boolean,
    recordingElapsedMs: Long,
    activeReplyState: ReplyState?,
    onCancelReply: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecordingAndSend: () -> Unit,
    onCancelRecording: () -> Unit,
    onSendText: (String) -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageComposer(
        inputText = inputText,
        onInputTextChanged = onInputTextChanged,
        onSendText = onSendText,
        onAttachClick = onAttachClick,
        onMicClick = {
            if (isRecording) {
                onStopRecordingAndSend()
            } else {
                onStartRecording()
            }
        },
        isRecording = isRecording,
        recordingElapsedMs = recordingElapsedMs,
        activeReplyState = activeReplyState,
        onCancelReply = onCancelReply,
        modifier = modifier
    )
}
