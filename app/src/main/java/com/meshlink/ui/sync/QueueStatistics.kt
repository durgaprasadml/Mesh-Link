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
 * QueueStatistics — Dashboard grid summarizing long-term queue metrics and dwell times.
 */
@Composable
fun QueueStatisticsCard(
    stats: QueueStatisticsUi,
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
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Queue Statistics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Queue Health & Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Score: ${stats.healthScore}/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    StatMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Pending,
                        label = "Pending",
                        value = "${stats.pending}",
                        accentColor = Color(0xFFFF9800)
                    )
                    StatMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = "Delivered",
                        value = "${stats.delivered}",
                        accentColor = Color(0xFF4CAF50)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    StatMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Cancel,
                        label = "Failed",
                        value = "${stats.failed}",
                        accentColor = Color(0xFFF44336)
                    )
                    StatMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Replay,
                        label = "Retried",
                        value = "${stats.retried}",
                        accentColor = Color(0xFF2196F3)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatMetricTile(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.AccessTime,
                        label = "Average Queue Dwell Time",
                        value = if (stats.avgQueueTimeMs > 0) "${stats.avgQueueTimeMs} ms" else "< 50 ms",
                        accentColor = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
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
                    .size(32.dp)
                    .clip(MeshTheme.shapes.small)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
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
            }
        }
    }
}
