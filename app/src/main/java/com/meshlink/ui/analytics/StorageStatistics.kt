package com.meshlink.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun StorageStatistics(
    storage: StorageStatisticsUi,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(MeshTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Storage & Cache Statistics",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                Text(
                    text = "Storage & Engine Caches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
            ) {
                StorageStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Route Entries",
                    value = storage.routeTableEntries.toString()
                )
                StorageStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Pending Queue",
                    value = storage.pendingQueueSize.toString()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
            ) {
                StorageStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Store & Forward",
                    value = storage.storeAndForwardCount.toString()
                )
                StorageStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Duplicate Cache",
                    value = storage.duplicateCacheSize.toString()
                )
            }
        }
    }
}

@Composable
private fun StorageStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MeshTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(MeshTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
