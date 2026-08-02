package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.TransferStatisticsUi

/**
 * Compact Material 3 Transfer Summary Dashboard component.
 * Displays Files Shared, Files Received, Active Transfers, and Total Storage Volume cleanly.
 */
@Composable
fun TransferSummary(
    statistics: TransferStatisticsUi,
    activeTransfersCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryMetricCard(
                label = "Shared",
                value = "${statistics.filesSentCount}",
                icon = Icons.Default.Upload,
                modifier = Modifier.weight(1f)
            )

            SummaryMetricCard(
                label = "Received",
                value = "${statistics.filesReceivedCount}",
                icon = Icons.Default.Download,
                modifier = Modifier.weight(1f)
            )

            SummaryMetricCard(
                label = "Active",
                value = "$activeTransfersCount",
                icon = Icons.Default.Sync,
                modifier = Modifier.weight(1f)
            )

            SummaryMetricCard(
                label = "Volume",
                value = statistics.totalVolumeFormatted,
                icon = Icons.Default.FolderZip,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MeshTheme.colors.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MeshTheme.colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.Bold),
            color = MeshTheme.colors.onSurface
        )
        Text(
            text = label,
            style = MeshTheme.customTypography.caption,
            color = MeshTheme.colors.onSurfaceVariant
        )
    }
}
