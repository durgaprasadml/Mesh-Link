package com.meshlink.ui.sync

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * DeliveryStatistics — Material 3 dashboard displaying Delivered, Relayed, Failed, Pending, and Success Rate with circular progress indicators.
 */
@Composable
fun DeliveryStatistics(
    deliveryUi: DeliveryUi,
    queueStatsUi: QueueStatisticsUi,
    modifier: Modifier = Modifier
) {
    val animatedSuccessRate by animateFloatAsState(
        targetValue = deliveryUi.successRate.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "SuccessRateProgress"
    )

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
                        contentDescription = "Delivery Statistics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Delivery Statistics & Reliability",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = MeshTheme.shapes.small,
                    color = Color(0xFF00E676).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(deliveryUi.successRate * 100).toInt()}% Success",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Success Rate Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedSuccessRate },
                        modifier = Modifier.fillMaxSize(),
                        color = if (animatedSuccessRate >= 0.85f) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeWidth = 8.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedSuccessRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fidelity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2x2 Grid of Delivery Counters
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "Delivered",
                            value = "${deliveryUi.totalDelivered}",
                            accentColor = Color(0xFF4CAF50)
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "Relayed",
                            value = "${deliveryUi.totalForwarded}",
                            accentColor = Color(0xFF2196F3)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "Pending",
                            value = "${queueStatsUi.pending}",
                            accentColor = Color(0xFFFF9800)
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "Failed",
                            value = "${deliveryUi.totalFailed}",
                            accentColor = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MeshTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
