package com.meshlink.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun NotificationSettingsSection(
    settings: NotificationSettingsUi,
    onSettingChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Notification & Alert Preferences",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            NotificationSwitchRow(
                title = "Direct Message Notifications",
                description = "Show popups and alerts for incoming peer messages",
                icon = Icons.Default.Message,
                checked = settings.messageAlerts,
                onCheckedChange = { onSettingChange("message_alerts", it) }
            )

            NotificationSwitchRow(
                title = "Community Broadcast Alerts",
                description = "Notify on mesh-wide channel broadcasts",
                icon = Icons.Default.Radio,
                checked = settings.broadcastAlerts,
                onCheckedChange = { onSettingChange("broadcast_alerts", it) }
            )

            NotificationSwitchRow(
                title = "Emergency SOS Alerts",
                description = "High priority emergency alerts (overrides quiet mode)",
                icon = Icons.Default.Warning,
                checked = settings.sosAlerts,
                onCheckedChange = { onSettingChange("sos_alerts", it) }
            )

            NotificationSwitchRow(
                title = "Nearby Peer Discovery",
                description = "Notify when new mesh nodes join range",
                icon = Icons.Default.Person,
                checked = settings.nearbyDiscovery,
                onCheckedChange = { onSettingChange("nearby_discovery", it) }
            )

            NotificationSwitchRow(
                title = "File & Media Transfer Progress",
                description = "Show foreground progress for active transfers",
                icon = Icons.Default.Download,
                checked = settings.transferAlerts,
                onCheckedChange = { onSettingChange("transfer_alerts", it) }
            )

            NotificationSwitchRow(
                title = "Mesh Analytics & Health Alerts",
                description = "Routing table, hop latency, and network diagnostics",
                icon = Icons.Default.Analytics,
                checked = settings.analyticsAlerts,
                onCheckedChange = { onSettingChange("analytics_alerts", it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            NotificationSwitchRow(
                title = "Sound Effects & Chimes",
                description = "Play custom acoustic chime on incoming notifications",
                icon = Icons.Default.VolumeUp,
                checked = settings.soundEnabled,
                onCheckedChange = { onSettingChange("sound_enabled", it) }
            )

            NotificationSwitchRow(
                title = "Haptic Vibration Patterns",
                description = "Tactile vibration feedback for alerts and messages",
                icon = Icons.Default.Vibration,
                checked = settings.vibrationEnabled,
                onCheckedChange = { onSettingChange("vibration_enabled", it) }
            )

            NotificationSwitchRow(
                title = "App Badge Unread Count",
                description = "Display unread notification badge count on app icon",
                icon = Icons.Default.Badge,
                checked = settings.badgeCountEnabled,
                onCheckedChange = { onSettingChange("badge_count_enabled", it) }
            )
        }
    }
}

@Composable
fun NotificationSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
