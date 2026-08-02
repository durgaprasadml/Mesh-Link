package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PermScanWifi
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Standardized Empty States for Mesh-Link Phase 15.
 * Reusable empty state components for Chats, Nearby, Broadcast, Analytics,
 * Media, Sync, Security, and SOS.
 */

enum class MeshEmptyStateCategory {
    CHATS,
    NEARBY,
    BROADCAST,
    ANALYTICS,
    MEDIA,
    SYNC,
    SECURITY,
    SOS
}

@Composable
fun MeshEmptyStateContainer(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconColor: Color = MeshTheme.colors.primary,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MeshTheme.typography.headlineSmall,
                color = MeshTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MeshTheme.typography.bodyMedium,
                color = MeshTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeshTheme.colors.primary,
                        contentColor = MeshTheme.colors.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }

            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = secondaryActionLabel, color = MeshTheme.colors.primary)
                }
            }
        }
    }
}

@Composable
fun MeshCategoryEmptyState(
    category: MeshEmptyStateCategory,
    modifier: Modifier = Modifier,
    onPrimaryAction: (() -> Unit)? = null
) {
    when (category) {
        MeshEmptyStateCategory.CHATS -> MeshEmptyStateContainer(
            title = "No Active Conversations",
            description = "Start a direct P2P chat with nearby mesh peers or join an active channel.",
            icon = Icons.Default.ChatBubbleOutline,
            actionLabel = "Discover Peers",
            onActionClick = onPrimaryAction,
            modifier = modifier
        )
        MeshEmptyStateCategory.NEARBY -> MeshEmptyStateContainer(
            title = "No Nearby Devices Discovered",
            description = "Make sure Bluetooth and Wi-Fi are enabled to discover nearby mesh nodes.",
            icon = Icons.Default.PermScanWifi,
            actionLabel = "Scan Mesh Network",
            onActionClick = onPrimaryAction,
            modifier = modifier
        )
        MeshEmptyStateCategory.BROADCAST -> MeshEmptyStateContainer(
            title = "No Mesh Broadcasts",
            description = "Channel broadcasts and peer announcements will appear here when transmitted.",
            icon = Icons.Default.Radio,
            actionLabel = "Send Broadcast",
            onActionClick = onPrimaryAction,
            modifier = modifier
        )
        MeshEmptyStateCategory.ANALYTICS -> MeshEmptyStateContainer(
            title = "Analytics Unavailable",
            description = "Real-time metrics will populate as network traffic flows through local nodes.",
            icon = Icons.Default.Analytics,
            modifier = modifier
        )
        MeshEmptyStateCategory.MEDIA -> MeshEmptyStateCategoryContainer(
            title = "No Transferred Media",
            description = "Shared photos, documents, and voice attachments will be listed here.",
            icon = Icons.Default.FolderZip,
            modifier = modifier
        )
        MeshEmptyStateCategory.SYNC -> MeshEmptyStateCategoryContainer(
            title = "Sync Queue Clear",
            description = "All offline mesh packets and messages have been synchronized.",
            icon = Icons.Default.CellTower,
            modifier = modifier
        )
        MeshEmptyStateCategory.SECURITY -> MeshEmptyStateCategoryContainer(
            title = "No Security Alerts",
            description = "Your encryption keys and peer verifications are currently up to date.",
            icon = Icons.Default.Security,
            modifier = modifier
        )
        MeshEmptyStateCategory.SOS -> MeshEmptyStateCategoryContainer(
            title = "No Emergency SOS Signals",
            description = "Active emergency beacons from nearby nodes will be displayed with high priority.",
            icon = Icons.Default.Warning,
            iconColor = MeshTheme.colors.error,
            modifier = modifier
        )
    }
}

@Composable
private fun MeshEmptyStateCategoryContainer(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconColor: Color = MeshTheme.colors.primary
) {
    MeshEmptyStateContainer(
        title = title,
        description = description,
        icon = icon,
        iconColor = iconColor,
        modifier = modifier
    )
}
