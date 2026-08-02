package com.meshlink.ui.sync

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
 * QueueOverview — Component 5 Material Cards showing Pending, Sending, Waiting, Delivered, Failed, Retrying with animated value counters.
 */
@Composable
fun QueueOverview(
    queueUi: QueueUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Queue,
                        contentDescription = "Queue Overview",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Message Queue Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${queueUi.totalQueueSize} Queue Size",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QueueItemRow(
                    label = "Pending",
                    count = queueUi.pendingCount,
                    description = "Waiting for network availability",
                    icon = Icons.Default.HourglassEmpty,
                    accentColor = Color(0xFFFF9800)
                )

                QueueItemRow(
                    label = "Sending",
                    count = queueUi.processingCount,
                    description = "Actively relaying over BLE / Wi-Fi Direct",
                    icon = Icons.Default.Sync,
                    accentColor = Color(0xFF2196F3)
                )

                QueueItemRow(
                    label = "Retrying",
                    count = queueUi.retryingCount,
                    description = "Exponential backoff recovery",
                    icon = Icons.Default.Replay,
                    accentColor = Color(0xFF9C27B0)
                )

                QueueItemRow(
                    label = "Delivered",
                    count = queueUi.completedCount,
                    description = "Successfully acknowledged by recipient",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF4CAF50)
                )

                QueueItemRow(
                    label = "Failed",
                    count = queueUi.failedCount,
                    description = "Max retry attempts exceeded",
                    icon = Icons.Default.ErrorOutline,
                    accentColor = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    label: String,
    count: Int,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(400),
        label = "QueueCount"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MeshTheme.shapes.small)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "$animatedCount",
            style = MaterialTheme.typography.titleMedium,
            color = if (animatedCount > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
