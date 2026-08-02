package com.meshlink.ui.designsystem.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Standard Material 3 Navigation Empty State Composable.
 */
@Composable
fun NavigationEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MeshTheme.colors.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MeshTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MeshTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MeshTheme.typography.bodyMedium,
            color = MeshTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                shape = MeshTheme.shapes.pill
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NoChatsEmptyState(
    modifier: Modifier = Modifier,
    onStartChat: (() -> Unit)? = null
) {
    NavigationEmptyState(
        icon = Icons.Filled.ChatBubbleOutline,
        title = "No Conversations Yet",
        subtitle = "Connect with nearby devices over offline P2P mesh to start messaging securely.",
        actionLabel = if (onStartChat != null) "Discover Nearby Devices" else null,
        onActionClick = onStartChat,
        modifier = modifier
    )
}

@Composable
fun NoNearbyDevicesEmptyState(
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null
) {
    NavigationEmptyState(
        icon = Icons.Filled.WifiOff,
        title = "No Mesh Devices Found",
        subtitle = "Scanning for Bluetooth LE and Wi-Fi Direct peers nearby. Ensure Wi-Fi and Bluetooth are enabled.",
        actionLabel = if (onRefresh != null) "Scan Again" else null,
        onActionClick = onRefresh,
        modifier = modifier
    )
}

@Composable
fun NoBroadcastsEmptyState(
    modifier: Modifier = Modifier,
    onCreateBroadcast: (() -> Unit)? = null
) {
    NavigationEmptyState(
        icon = Icons.Filled.RssFeed,
        title = "No Active Broadcasts",
        subtitle = "Broadcast emergency bulletins, alerts, or public messages to all nodes in range.",
        actionLabel = if (onCreateBroadcast != null) "Create Broadcast" else null,
        onActionClick = onCreateBroadcast,
        modifier = modifier
    )
}

@Composable
fun NoSosAlertsEmptyState(
    modifier: Modifier = Modifier
) {
    NavigationEmptyState(
        icon = Icons.Filled.WarningAmber,
        title = "No Emergency SOS Alerts",
        subtitle = "Emergency distress beacons broadcasted across the mesh will be prioritized here.",
        modifier = modifier
    )
}

@Composable
fun NoAnalyticsEmptyState(
    modifier: Modifier = Modifier
) {
    NavigationEmptyState(
        icon = Icons.Filled.Analytics,
        title = "No Telemetry Data",
        subtitle = "Mesh routing metrics, peer hops, and bandwidth diagnostics will populate automatically.",
        modifier = modifier
    )
}

@Composable
fun NoMessagesEmptyState(
    modifier: Modifier = Modifier
) {
    NavigationEmptyState(
        icon = Icons.Filled.SignalCellularOff,
        title = "No Messages",
        subtitle = "Send your first end-to-end encrypted mesh message below.",
        modifier = modifier
    )
}
