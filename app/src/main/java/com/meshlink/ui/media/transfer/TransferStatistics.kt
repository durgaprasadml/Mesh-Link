package com.meshlink.ui.media.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.TransferStatisticsUi

/**
 * Dashboard telemetry metrics card for mesh media and file transfer engine.
 */
@Composable
fun TransferStatisticsDashboard(
    stats: TransferStatisticsUi,
    modifier: Modifier = Modifier
) {
    MeshCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transfer Telemetry Dashboard",
                        style = MeshTheme.customTypography.title,
                        color = MeshTheme.colors.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MeshTheme.colors.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = stats.transportModeName,
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                        color = MeshTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Telemetry Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Files Sent",
                    value = "${stats.filesSentCount}",
                    icon = Icons.Default.FileUpload,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Files Received",
                    value = "${stats.filesReceivedCount}",
                    icon = Icons.Default.FileDownload,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Total Volume",
                    value = stats.totalVolumeFormatted,
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Success Rate",
                    value = "${stats.successRatePercent}%",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transport Split Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "BLE",
                        tint = MeshTheme.colors.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BLE: ${stats.bleTransfersCount}",
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Medium),
                        color = MeshTheme.colors.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi Direct",
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wi-Fi Direct: ${stats.wifiTransfersCount}",
                        style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Medium),
                        color = MeshTheme.colors.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MeshTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.Bold),
                    color = MeshTheme.colors.onSurface
                )
                Text(
                    text = title,
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}
