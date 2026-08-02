package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.settings.SettingsUiState

@Composable
fun NotificationSection(
    uiState: SettingsUiState,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val notificationItems = listOf(
            SettingsItemUi(
                id = "msg_notif",
                title = "Message Notifications",
                subtitle = "Receive push alerts for mesh direct messages",
                icon = Icons.Default.Message,
                isChecked = uiState.messageNotifications,
                onClick = { onShowToast("Message notification preference toggled") }
            ),
            SettingsItemUi(
                id = "sos_alerts",
                title = "Emergency SOS Alerts",
                subtitle = "High-priority sound and vibration for broadcast SOS",
                icon = Icons.Default.WarningAmber,
                isChecked = uiState.sosAlertsEnabled,
                onClick = { onShowToast("SOS alert preference toggled") }
            ),
            SettingsItemUi(
                id = "broadcast_alerts",
                title = "Nearby Broadcast Alerts",
                subtitle = "Alert on general community mesh broadcasts",
                icon = Icons.Default.Campaign,
                isChecked = true,
                onClick = { onShowToast("Broadcast alert preference toggled") }
            ),
            SettingsItemUi(
                id = "vibration",
                title = "Haptic Vibration Pulse",
                subtitle = "Vibrate device on packet delivery",
                icon = Icons.Default.Vibration,
                isChecked = uiState.vibrationEnabled,
                onClick = { onShowToast("Vibration feedback preference toggled") }
            ),
            SettingsItemUi(
                id = "led_flash",
                title = "LED Flash Indicator",
                subtitle = "Flash camera LED for emergency packets",
                icon = Icons.Default.FlashOn,
                isChecked = uiState.ledFlashEnabled,
                onClick = { onShowToast("LED flash preference toggled") }
            ),
            SettingsItemUi(
                id = "sound_tone",
                title = "Notification Tone",
                subtitle = uiState.notificationSound,
                icon = Icons.Default.Notifications,
                trailingText = uiState.notificationSound,
                onClick = { onShowToast("Selected tone: ${uiState.notificationSound}") }
            )
        )

        SettingsGroupCard(
            title = "Notifications & Tactical Alerts",
            items = notificationItems
        )
    }
}
