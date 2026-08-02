package com.meshlink.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * OfflineBanner — Persistent banner displaying offline, recovery, or mesh state.
 */
@Composable
fun OfflineBanner(
    syncUi: SyncUi,
    recoveryUi: MeshRecoveryUi,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bannerConfig = when {
        recoveryUi.isReconnecting -> BannerStateConfig(
            title = "Recovery in Progress",
            description = "Attempting mesh reconnection and rebuilding route table...",
            icon = Icons.Default.Autorenew,
            backgroundColor = Color(0xFF1976D2),
            contentColor = Color.White
        )
        syncUi.isOffline -> BannerStateConfig(
            title = "Offline Mode — Store & Forward Active",
            description = "No direct peers connected. Outgoing messages are queued safely.",
            icon = Icons.Default.WifiOff,
            backgroundColor = Color(0xFFE65100),
            contentColor = Color.White
        )
        syncUi.isSyncing -> BannerStateConfig(
            title = "Mesh Synchronization Active",
            description = "${syncUi.remainingItems} items remaining (${String.format("%.1f", syncUi.speedKbps)} KB/s)",
            icon = Icons.Default.Sync,
            backgroundColor = Color(0xFF0288D1),
            contentColor = Color.White
        )
        !recoveryUi.isMeshRestored -> BannerStateConfig(
            title = "Waiting for Peers",
            description = "Peer routes interrupted. Scanning BLE & Wi-Fi Direct interfaces...",
            icon = Icons.Default.Search,
            backgroundColor = Color(0xFFF57C00),
            contentColor = Color.White
        )
        else -> BannerStateConfig(
            title = "Mesh Network Available",
            description = "Fully connected. Direct peer delivery & relay enabled.",
            icon = Icons.Default.CheckCircle,
            backgroundColor = Color(0xFF2E7D32),
            contentColor = Color.White
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offlineFadeAnimation(syncUi.isOffline),
        shape = MeshTheme.shapes.medium,
        color = bannerConfig.backgroundColor.copy(alpha = 0.92f),
        tonalElevation = MeshTheme.elevation.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshSpacing.CardInternalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .reconnectRippleAnimation(recoveryUi.isReconnecting),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = bannerConfig.icon,
                    contentDescription = bannerConfig.title,
                    tint = bannerConfig.contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bannerConfig.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = bannerConfig.contentColor
                )
                Text(
                    text = bannerConfig.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = bannerConfig.contentColor.copy(alpha = 0.85f)
                )
            }

            if (onRetryClick != null && (syncUi.isOffline || !recoveryUi.isMeshRestored)) {
                OutlinedButton(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = bannerConfig.contentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private data class BannerStateConfig(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val contentColor: Color
)
