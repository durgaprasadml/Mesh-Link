package com.meshlink.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun BackgroundServicesDashboard(
    services: List<BackgroundServiceUi>,
    onToggleService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background Services",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${services.count { it.state == ServiceState.RUNNING }}/${services.size} Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            services.forEach { service ->
                BackgroundServiceItemRow(
                    service = service,
                    onToggle = { onToggleService(service.serviceId) }
                )
            }
        }
    }
}

@Composable
fun BackgroundServiceItemRow(
    service: BackgroundServiceUi,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when {
        service.serviceName.contains("Mesh", ignoreCase = true) -> Icons.Default.CellTower
        service.serviceName.contains("Discovery", ignoreCase = true) -> Icons.Default.Radar
        service.serviceName.contains("Sync", ignoreCase = true) -> Icons.Default.Sync
        service.serviceName.contains("Transfer", ignoreCase = true) -> Icons.Default.FolderZip
        else -> Icons.Default.Refresh
    }

    val (statusColor, statusText) = when (service.state) {
        ServiceState.RUNNING -> Pair(MaterialTheme.colorScheme.primary, "Running")
        ServiceState.PAUSED -> Pair(MaterialTheme.colorScheme.tertiary, "Paused")
        ServiceState.STOPPED -> Pair(MaterialTheme.colorScheme.error, "Stopped")
        ServiceState.WAITING -> Pair(MaterialTheme.colorScheme.secondary, "Waiting")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = service.serviceName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = service.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = service.uptime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
