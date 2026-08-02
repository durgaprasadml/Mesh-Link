package com.meshlink.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun NetworkOverview(
    health: MeshHealthUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
    ) {
        Text(
            text = "Tactical Network Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                title = "Connected Nodes",
                value = health.activeNodesCount.toString(),
                icon = Icons.Default.Hub,
                color = MeshTheme.colors.info
            )
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                title = "Active Routes",
                value = health.activeRoutesCount.toString(),
                icon = Icons.AutoMirrored.Filled.AltRoute,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                title = "Relay Nodes",
                value = health.relayNodesCount.toString(),
                icon = Icons.Default.SyncAlt,
                color = MaterialTheme.colorScheme.secondary
            )
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                title = "Health Score",
                value = "${health.healthScore}%",
                icon = Icons.Default.CheckCircle,
                color = if (health.healthScore >= 75) MeshTheme.colors.success else MeshTheme.colors.warning
            )
        }
    }
}

@Composable
fun OverviewMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedVal = rememberAnimatedCounter(
        targetValue = value.filter { it.isDigit() }.toIntOrNull() ?: 0
    )
    val displayValue = if (value.endsWith("%")) "${animatedVal.value}%" else if (value.filter { it.isDigit() }.isNotEmpty()) animatedVal.value.toString() else value

    Card(
        modifier = modifier,
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .padding(MeshTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
            Text(
                text = displayValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
