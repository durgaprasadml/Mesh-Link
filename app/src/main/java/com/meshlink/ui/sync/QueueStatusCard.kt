package com.meshlink.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
 * QueueStatusCard — Detailed view of Pending, Retry, Failed, Processing, and Completed message queues.
 */
@Composable
fun QueueStatusCard(
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
                        contentDescription = "Queue Status",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Message Queue Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${queueUi.totalQueueSize} Active Items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Queue breakdown list
            QueueRowItem(
                label = "Pending Queue",
                count = queueUi.pendingCount,
                description = "Awaiting available route to recipient",
                icon = Icons.Default.HourglassEmpty,
                accentColor = Color(0xFFFF9800)
            )

            QueueRowItem(
                label = "Retry Queue",
                count = queueUi.retryingCount,
                description = "Under exponential backoff recovery",
                icon = Icons.Default.Replay,
                accentColor = Color(0xFF2196F3)
            )

            QueueRowItem(
                label = "Processing Queue",
                count = queueUi.processingCount,
                description = "Currently packaging or transmitting",
                icon = Icons.Default.Sync,
                accentColor = Color(0xFF9C27B0)
            )

            QueueRowItem(
                label = "Failed Queue",
                count = queueUi.failedCount,
                description = "Exceeded maximum retry attempts",
                icon = Icons.Default.ErrorOutline,
                accentColor = Color(0xFFF44336)
            )

            QueueRowItem(
                label = "Completed Queue",
                count = queueUi.completedCount,
                description = "Successfully delivered to destination",
                icon = Icons.Default.CheckCircleOutline,
                accentColor = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun QueueRowItem(
    label: String,
    count: Int,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
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
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            color = if (count > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
