package com.meshlink.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationSettings(
    uiState: SettingsUiState,
    onToggleMessageNotifications: (Boolean) -> Unit = {},
    onToggleBroadcastNotifications: (Boolean) -> Unit = {},
    onToggleNearbyNotifications: (Boolean) -> Unit = {},
    onToggleSosAlerts: (Boolean) -> Unit = {},
    onToggleVibration: (Boolean) -> Unit = {},
    onToggleBadges: (Boolean) -> Unit = {},
    onNavigateToSoundPicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Notification Preferences",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    title = "Message Alerts",
                    subtitle = "Alerts for direct P2P mesh messages",
                    isSwitch = true,
                    isChecked = uiState.messageNotifications,
                    onCheckedChange = onToggleMessageNotifications
                )

                SettingsRow(
                    icon = Icons.Default.Campaign,
                    title = "Broadcast Channels",
                    subtitle = "Alerts for multi-hop community broadcasts",
                    isSwitch = true,
                    isChecked = true,
                    onCheckedChange = onToggleBroadcastNotifications
                )

                SettingsRow(
                    icon = Icons.Default.Radar,
                    title = "Nearby Devices Discovered",
                    subtitle = "Alert when new mesh peer comes into range",
                    isSwitch = true,
                    isChecked = true,
                    onCheckedChange = onToggleNearbyNotifications
                )

                SettingsRow(
                    icon = Icons.Default.Warning,
                    title = "Emergency SOS Alerts",
                    subtitle = "High-priority distress broadcasts and sound override",
                    isSwitch = true,
                    isChecked = uiState.sosAlertsEnabled,
                    onCheckedChange = onToggleSosAlerts
                )

                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Notification Sound",
                    subtitle = uiState.notificationSound,
                    onClick = onNavigateToSoundPicker
                )

                SettingsRow(
                    icon = Icons.Default.Vibration,
                    title = "Vibration & Haptics",
                    subtitle = "Tactile haptic pulses for mesh events",
                    isSwitch = true,
                    isChecked = uiState.vibrationEnabled,
                    onCheckedChange = onToggleVibration
                )

                SettingsRow(
                    icon = Icons.Default.Badge,
                    title = "App Icon Badges",
                    subtitle = "Display unread message counter badge on app icon",
                    isSwitch = true,
                    isChecked = true,
                    onCheckedChange = onToggleBadges,
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NotificationPreview(
            messageEnabled = uiState.messageNotifications,
            broadcastEnabled = true,
            sosAlertsEnabled = uiState.sosAlertsEnabled,
            soundName = uiState.notificationSound
        )
    }
}
