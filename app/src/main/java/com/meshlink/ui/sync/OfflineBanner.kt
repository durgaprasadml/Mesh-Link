package com.meshlink.ui.sync

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
 * BannerStateCategory representing discrete network & sync operational states.
 */
enum class BannerStateCategory {
    ONLINE,
    OFFLINE,
    WAITING_FOR_PEERS,
    MESH_AVAILABLE,
    SYNC_PAUSED,
    RECOVERING
}

/**
 * OfflineBanner — Persistent status banner displaying online, offline, pause, waiting, or recovery state.
 */
@Composable
fun OfflineBanner(
    syncUi: SyncUi,
    recoveryUi: MeshRecoveryUi,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bannerCategory = when {
        recoveryUi.isReconnecting -> BannerStateCategory.RECOVERING
        syncUi.isOffline -> BannerStateCategory.OFFLINE
        syncUi.isSyncing -> BannerStateCategory.ONLINE
        !recoveryUi.isMeshRestored -> BannerStateCategory.WAITING_FOR_PEERS
        else -> BannerStateCategory.MESH_AVAILABLE
    }

    val bannerConfig = when (bannerCategory) {
        BannerStateCategory.RECOVERING -> BannerStateConfig(
            title = "Recovery in Progress",
            description = "Rebuilding mesh route topology and discovering peers...",
            icon = Icons.Default.Autorenew,
            backgroundColor = Color(0xFF1565C0),
            contentColor = Color.White
        )
        BannerStateCategory.OFFLINE -> BannerStateConfig(
            title = "Offline Mode — Store & Forward Active",
            description = "No direct peers reachable. Outgoing messages are queued safely.",
            icon = Icons.Default.WifiOff,
            backgroundColor = Color(0xFFD84315),
            contentColor = Color.White
        )
        BannerStateCategory.ONLINE -> BannerStateConfig(
            title = "Mesh Synchronization Active",
            description = "${syncUi.remainingItems} items pending (${String.format("%.1f", syncUi.speedKbps)} KB/s)",
            icon = Icons.Default.Sync,
            backgroundColor = Color(0xFF0277BD),
            contentColor = Color.White
        )
        BannerStateCategory.WAITING_FOR_PEERS -> BannerStateConfig(
            title = "Waiting for Peers",
            description = "Scanning BLE & Wi-Fi Direct interfaces for available mesh routes...",
            icon = Icons.Default.Search,
            backgroundColor = Color(0xFFEF6C00),
            contentColor = Color.White
        )
        BannerStateCategory.SYNC_PAUSED -> BannerStateConfig(
            title = "Synchronization Paused",
            description = "Sync paused to preserve energy. Tap refresh to resume.",
            icon = Icons.Default.PauseCircle,
            backgroundColor = Color(0xFF455A64),
            contentColor = Color.White
        )
        BannerStateCategory.MESH_AVAILABLE -> BannerStateConfig(
            title = "Mesh Network Available",
            description = "Connected to mesh network. Real-time peer relay active.",
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
        color = bannerConfig.backgroundColor.copy(alpha = 0.95f),
        tonalElevation = MeshTheme.elevation.card
    ) {
        AnimatedContent(
            targetState = bannerConfig,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "BannerTransition"
        ) { config ->
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
                        .reconnectRippleAnimation(recoveryUi.isReconnecting)
                        .syncSpinnerAnimation(syncUi.isSyncing && config.icon == Icons.Default.Sync),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.title,
                        tint = config.contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = config.contentColor
                    )
                    Text(
                        text = config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = config.contentColor.copy(alpha = 0.88f)
                    )
                }

                if (onRetryClick != null) {
                    IconButton(
                        onClick = onRetryClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Banner State",
                            tint = config.contentColor
                        )
                    }
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
