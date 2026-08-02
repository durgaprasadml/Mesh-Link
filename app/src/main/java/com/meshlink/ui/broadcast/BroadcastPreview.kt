package com.meshlink.ui.broadcast

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.designsystem.theme.MeshTheme

private val sampleMessages = listOf(
    Message(
        messageId = "msg_001",
        chatId = "broadcast",
        text = "[EMERGENCY] SOS! Need assistance at Sector 4 Base Station.",
        senderId = "node_alpha",
        timestamp = System.currentTimeMillis() - 3600000,
        isFromMe = false,
        status = DeliveryStatus.DELIVERED,
        messageType = MessageType.SOS,
        latitude = 37.7749,
        longitude = -122.4194
    ),
    Message(
        messageId = "msg_002",
        chatId = "broadcast",
        text = "Mesh network operational. 12 active relay nodes connected.",
        senderId = "node_me",
        timestamp = System.currentTimeMillis() - 1800000,
        isFromMe = true,
        status = DeliveryStatus.SENT,
        messageType = MessageType.TEXT
    )
)

private val samplePeers = mapOf(
    "node_alpha" to UserIdentity.create("node_alpha", "Tactical Alpha Node"),
    "node_beta" to UserIdentity.create("node_beta", "Bravo Command")
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun BroadcastScreenPreview() {
    MeshTheme {
        MeshBroadcastScreen(
            messages = sampleMessages,
            peerIdentities = samplePeers,
            onBack = {},
            onSendBroadcast = {}
        )
    }
}

@Preview(name = "Tablet View", device = Devices.TABLET, showBackground = true)
@Composable
fun BroadcastTabletPreview() {
    MeshTheme {
        MeshBroadcastScreen(
            messages = sampleMessages,
            peerIdentities = samplePeers,
            onBack = {},
            onSendBroadcast = {}
        )
    }
}

@Preview(name = "Foldable View", device = Devices.FOLDABLE, showBackground = true)
@Composable
fun BroadcastFoldablePreview() {
    MeshTheme {
        MeshBroadcastScreen(
            messages = sampleMessages,
            peerIdentities = samplePeers,
            onBack = {},
            onSendBroadcast = {}
        )
    }
}
