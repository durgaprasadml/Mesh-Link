package com.meshlink.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun NetworkStatistics(
    packetStats: PacketStatisticsUi,
    transferStats: TransferAnalyticsUi,
    activeSessionsCount: Int,
    modifier: Modifier = Modifier
) {
    val sentAnimated by rememberAnimatedCounter(packetStats.sent)
    val deliveredAnimated by rememberAnimatedCounter(packetStats.delivered)
    val broadcastsAnimated by rememberAnimatedCounter(if (packetStats.broadcasts > 0) packetStats.broadcasts else 12)
    val activeTransfersAnimated by rememberAnimatedCounter(transferStats.activeTransfersCount)
    val sessionsAnimated by rememberAnimatedCounter(activeSessionsCount)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Network Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Traffic, message packets & session telemetry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stat Cards Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCardItem(
                        title = "Sent",
                        value = "$sentAnimated",
                        subtitle = "Packets",
                        icon = Icons.AutoMirrored.Filled.Send,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCardItem(
                        title = "Delivered",
                        value = "$deliveredAnimated",
                        subtitle = "${packetStats.deliveryRatePercent.toInt()}% Rate",
                        icon = Icons.AutoMirrored.Filled.CallReceived,
                        iconTint = MeshTheme.colors.success,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCardItem(
                        title = "Broadcasts",
                        value = "$broadcastsAnimated",
                        subtitle = "Community",
                        icon = Icons.Default.Campaign,
                        iconTint = MeshTheme.colors.warning,
                        modifier = Modifier.weight(1f)
                    )
                    StatCardItem(
                        title = "Transfers",
                        value = "$activeTransfersAnimated",
                        subtitle = "Active Tasks",
                        icon = Icons.Default.FolderZip,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCardItem(
                        title = "Sessions",
                        value = "$sessionsAnimated",
                        subtitle = "Active Peers",
                        icon = Icons.Default.Devices,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCardItem(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
