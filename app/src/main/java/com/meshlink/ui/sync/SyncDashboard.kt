package com.meshlink.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * SyncDashboard — Mission Control style header & status dashboard for Mesh-Link Sync.
 */
@Composable
fun SyncDashboard(
    syncUi: SyncUi,
    queueUi: QueueUi,
    deliveryUi: DeliveryUi,
    onForceSyncClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = MeshTheme.elevation.card,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            // Header Row: Network & Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    syncUi.isOffline -> Color(0xFFFF9800)
                                    syncUi.isSyncing -> Color(0xFF2196F3)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                    )
                    Column {
                        Text(
                            text = if (syncUi.isOffline) "OFFLINE MESH MODE" else "MISSION CONTROL SYNC",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = syncUi.statusMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (onForceSyncClick != null) {
                    FilledTonalButton(
                        onClick = onForceSyncClick,
                        enabled = !syncUi.isSyncing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Now",
                            modifier = Modifier
                                .size(16.dp)
                                .syncSpinnerAnimation(syncUi.isSyncing)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (syncUi.isSyncing) "Syncing..." else "Sync Now",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2x3 Grid of Status Cards
            Column(verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    DashboardMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CloudSync,
                        label = "Sync Status",
                        value = if (syncUi.isSyncing) "${(syncUi.progressFraction * 100).toInt()}%" else "Synced",
                        subtitle = syncUi.currentPhase,
                        accentColor = if (syncUi.isSyncing) Color(0xFF2196F3) else Color(0xFF4CAF50)
                    )

                    DashboardMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MarkAsUnread,
                        label = "Pending Queue",
                        value = "${queueUi.pendingCount}",
                        subtitle = "${queueUi.processingCount} Processing",
                        accentColor = if (queueUi.pendingCount > 0) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    DashboardMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        label = "Queue Size",
                        value = "${queueUi.totalQueueSize}",
                        subtitle = "${queueUi.failedCount} Failed / ${queueUi.retryingCount} Retrying",
                        accentColor = if (queueUi.failedCount > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                    )

                    DashboardMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Send,
                        label = "Active Deliveries",
                        value = "${deliveryUi.activeDeliveries}",
                        subtitle = "${deliveryUi.totalDelivered} Delivered",
                        accentColor = Color(0xFF00F59B)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    DashboardMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Hub,
                        label = "Active Relays",
                        value = "${deliveryUi.activeRelays}",
                        subtitle = "${deliveryUi.totalForwarded} Forwarded",
                        accentColor = Color(0xFF9C27B0)
                    )

                    DashboardMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = "Success Rate",
                        value = "${(deliveryUi.successRate * 100).toInt()}%",
                        subtitle = "Delivery Fidelity",
                        accentColor = if (deliveryUi.successRate >= 0.9f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MeshTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MeshTheme.shapes.small)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
