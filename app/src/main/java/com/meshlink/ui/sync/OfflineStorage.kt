package com.meshlink.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
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
 * OfflineStorage — Component 13 Storage Cache breakdown with segmented progress bars.
 */
@Composable
fun OfflineStorageCard(
    offlineUi: OfflineUi,
    modifier: Modifier = Modifier
) {
    val totalItems = (offlineUi.cachedMessagesCount + offlineUi.pendingUploadsCount + offlineUi.pendingDownloadsCount).coerceAtLeast(1)
    val cachedWeight = offlineUi.cachedMessagesCount.toFloat() / totalItems
    val uploadWeight = offlineUi.pendingUploadsCount.toFloat() / totalItems
    val downloadWeight = offlineUi.pendingDownloadsCount.toFloat() / totalItems

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
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage Overview",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Offline Storage & Cache Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = formatBytes(offlineUi.localStorageBytes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Segmented Progress Bar Visualization
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Cache Memory Segment Distribution",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MeshTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    if (cachedWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(cachedWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF2196F3))
                        )
                    }
                    if (uploadWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(uploadWeight)
                                .fillMaxHeight()
                                .background(Color(0xFFFF9800))
                        )
                    }
                    if (downloadWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(downloadWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF9C27B0))
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    StorageTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Folder,
                        label = "Cached Messages",
                        value = "${offlineUi.cachedMessagesCount}",
                        accentColor = Color(0xFF2196F3)
                    )
                    StorageTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CloudUpload,
                        label = "Pending Uploads",
                        value = "${offlineUi.pendingUploadsCount}",
                        accentColor = Color(0xFFFF9800)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    StorageTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CloudDownload,
                        label = "Pending Downloads",
                        value = "${offlineUi.pendingDownloadsCount}",
                        accentColor = Color(0xFF9C27B0)
                    )
                    StorageTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Storage,
                        label = "Local Storage",
                        value = formatBytes(offlineUi.localStorageBytes),
                        accentColor = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageTile(
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024f
    if (kb < 1024) return "${String.format("%.1f", kb)} KB"
    val mb = kb / 1024f
    return "${String.format("%.1f", mb)} MB"
}

/**
 * OfflineStorage — Alias for OfflineStorageCard for component name consistency.
 */
@Composable
fun OfflineStorage(
    offlineUi: OfflineUi,
    modifier: Modifier = Modifier
) {
    OfflineStorageCard(offlineUi = offlineUi, modifier = modifier)
}
