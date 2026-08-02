package com.meshlink.ui.broadcast

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.DashboardCard
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun BroadcastStatistics(
    stats: BroadcastStatisticsUi,
    onFilterClick: (BroadcastFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumSmall)
    ) {
        DashboardCard(
            icon = Icons.Default.Campaign,
            title = "${stats.totalBroadcasts} Broadcasts",
            subtitle = "Total network posts",
            onClick = { onFilterClick(BroadcastFilterState()) },
            modifier = Modifier.weight(1f),
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconTintColor = MaterialTheme.colorScheme.primary,
            isActive = true
        )

        DashboardCard(
            icon = Icons.Default.CheckCircle,
            title = "${stats.successRatePercentage}% Success",
            subtitle = "${stats.deliveredCount} delivered",
            onClick = { onFilterClick(BroadcastFilterState(selectedDeliveryState = BroadcastDeliveryState.DELIVERED)) },
            modifier = Modifier.weight(1f),
            iconContainerColor = Color(0xFF00F59B).copy(alpha = 0.2f),
            iconTintColor = Color(0xFF00F59B),
            isActive = stats.deliveredCount > 0
        )

        if (stats.emergencyCount > 0) {
            DashboardCard(
                icon = Icons.Default.Warning,
                title = "${stats.emergencyCount} Emergency",
                subtitle = "SOS broadcasts",
                onClick = { onFilterClick(BroadcastFilterState(emergencyOnly = true)) },
                modifier = Modifier.weight(1f),
                iconContainerColor = Color(0xFFFF0055).copy(alpha = 0.2f),
                iconTintColor = Color(0xFFFF0055),
                badgeCount = stats.emergencyCount,
                isActive = true
            )
        }
    }
}
