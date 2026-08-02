package com.meshlink.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.UserIdentity
import com.meshlink.messaging.presentation.ChatDetailUiState
import com.meshlink.messaging.presentation.ConnectionState
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Compose `@Preview` suite for Mesh-Link Phase 5 Chat UI.
 */

private val mockPeerIdentity = UserIdentity.create(
    userId = "node_alpha_123",
    displayName = "Tactical Operator"
)

private val mockMessages = listOf(
    Message(
        messageId = "1",
        chatId = "node_alpha_123",
        text = "Establishing P2P link over BLE mesh...",
        senderId = "node_alpha_123",
        timestamp = System.currentTimeMillis() - 3600000,
        isFromMe = false,
        status = DeliveryStatus.DELIVERED,
        messageType = MessageType.TEXT
    ),
    Message(
        messageId = "2",
        chatId = "node_alpha_123",
        text = "Copy that. Link secured with E2EE payload.",
        senderId = "me",
        timestamp = System.currentTimeMillis() - 1800000,
        isFromMe = true,
        status = DeliveryStatus.SEEN,
        messageType = MessageType.TEXT
    ),
    Message(
        messageId = "3",
        chatId = "node_alpha_123",
        text = "Grid status normal. 4 relay hops active.",
        senderId = "node_alpha_123",
        timestamp = System.currentTimeMillis() - 600000,
        isFromMe = false,
        status = DeliveryStatus.RELAYED,
        messageType = MessageType.TEXT
    )
)

@Preview(name = "Chat Screen - Dark Theme", showBackground = true, device = Devices.PIXEL_7)
@Composable
fun ChatScreenDarkPreview() {
    MeshTheme(themeMode = "DARK") {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatScreen(
                peerIdentity = mockPeerIdentity,
                peerAddress = "node_alpha_123",
                fallbackName = "Tactical Operator",
                uiState = ChatDetailUiState(
                    messages = mockMessages,
                    connectionStatus = ConnectionState.DIRECT
                ),
                onBack = {},
                onSendMessage = {},
                onSendImage = {},
                onSendLocation = {},
                onStartRecording = {},
                onStopRecordingAndSend = {},
                onCancelRecording = {},
                onToggleMessageSelection = {},
                onClearSelection = {},
                onDeleteSelectedMessages = {},
                onDeleteChat = {},
                onPlayVoice = {},
                onStopPlayback = {},
                onRetryTransfer = {},
                onOpenLocation = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Chat Screen - Light Theme", showBackground = true, device = Devices.PIXEL_7)
@Composable
fun ChatScreenLightPreview() {
    MeshTheme(themeMode = "LIGHT") {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatScreen(
                peerIdentity = mockPeerIdentity,
                peerAddress = "node_alpha_123",
                fallbackName = "Tactical Operator",
                uiState = ChatDetailUiState(
                    messages = mockMessages,
                    connectionStatus = ConnectionState.RELAY
                ),
                onBack = {},
                onSendMessage = {},
                onSendImage = {},
                onSendLocation = {},
                onStartRecording = {},
                onStopRecordingAndSend = {},
                onCancelRecording = {},
                onToggleMessageSelection = {},
                onClearSelection = {},
                onDeleteSelectedMessages = {},
                onDeleteChat = {},
                onPlayVoice = {},
                onStopPlayback = {},
                onRetryTransfer = {},
                onOpenLocation = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Chat Screen - Tablet Layout", showBackground = true, device = Devices.PIXEL_TABLET)
@Composable
fun ChatScreenTabletPreview() {
    MeshTheme(themeMode = "DARK") {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatScreen(
                peerIdentity = mockPeerIdentity,
                peerAddress = "node_alpha_123",
                fallbackName = "Tactical Operator",
                uiState = ChatDetailUiState(
                    messages = mockMessages,
                    connectionStatus = ConnectionState.DIRECT
                ),
                onBack = {},
                onSendMessage = {},
                onSendImage = {},
                onSendLocation = {},
                onStartRecording = {},
                onStopRecordingAndSend = {},
                onCancelRecording = {},
                onToggleMessageSelection = {},
                onClearSelection = {},
                onDeleteSelectedMessages = {},
                onDeleteChat = {},
                onPlayVoice = {},
                onStopPlayback = {},
                onRetryTransfer = {},
                onOpenLocation = { _, _ -> }
            )
        }
    }
}
