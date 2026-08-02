package com.meshlink.ui.sync

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * MeshHealthDashboard — Modern Material 3 dashboard card displaying Network Health,
 * Connected Peers, Relay Nodes, Active Deliveries, Queue Health, and Sync Status with animated counters.
 */
@Composable
fun MeshHealthDashboard(
    syncUi: SyncUi,
    queueUi: QueueUi,
    deliveryUi: DeliveryUi,
    queueStatsUi: QueueStatisticsUi,
    onForceSyncClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animatedHealthScore by animateIntAsState(
        targetValue = queueStatsUi.healthScore.coerceIn(0, 100),
        animationSpec = tween(500),
        label = "HealthScore"
    )

    val animatedPeersCount by animateIntAsState(
        targetValue = deliveryUi.activeRelays,
        animationSpec = tween(500),
        label = "PeersCount"
    )

    val animatedDeliveries by animateIntAsState(
        targetValue = deliveryUi.activeDeliveries,
        animationSpec = tween(500),
        label = "DeliveriesCount"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = MeshTheme.elevation.card,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            // Header Row: Network Health Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            text = "MESH HEALTH & RELIABILITY",
                            style = MaterialTheme.typography.labelSmall,
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

            // Metrics Grid
            Column(verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    HealthMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Favorite,
                        label = "Network Health",
                        value = "$animatedHealthScore%",
                        subtitle = if (animatedHealthScore >= 80) "Optimal Topology" else "Degraded Topology",
                        accentColor = if (animatedHealthScore >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )

                    HealthMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        label = "Connected Peers",
                        value = "$animatedPeersCount",
                        subtitle = "${deliveryUi.activeRelays} Relays Available",
                        accentColor = Color(0xFF2196F3)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    HealthMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MoveToInbox,
                        label = "Active Deliveries",
                        value = "$animatedDeliveries",
                        subtitle = "${queueUi.pendingCount} Queued",
                        accentColor = Color(0xFF00E676)
                    )

                    HealthMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = "Sync Status",
                        value = if (syncUi.isSyncing) "${(syncUi.progressFraction * 100).toInt()}%" else "Synced",
                        subtitle = syncUi.currentPhase,
                        accentColor = if (syncUi.isSyncing) Color(0xFF0288D1) else Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthMetricTile(
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
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
