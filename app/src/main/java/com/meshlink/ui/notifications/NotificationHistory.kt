package com.meshlink.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun NotificationHistorySection(
    historyItems: List<NotificationHistoryUi>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (historyItems.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Notification Log",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(onClick = onClearHistory) {
                    Text("Clear Log")
                }
            }

            historyItems.forEach { item ->
                NotificationHistoryRow(item = item)
            }
        }
    }
}

@Composable
fun NotificationHistoryRow(
    item: NotificationHistoryUi,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusIcon) = when (item.statusBadge) {
        NotificationStatus.DELIVERED -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        NotificationStatus.READ -> Pair(MaterialTheme.colorScheme.tertiary, Icons.Default.CheckCircle)
        NotificationStatus.DISMISSED -> Pair(MaterialTheme.colorScheme.outline, Icons.Default.RemoveCircle)
        NotificationStatus.MISSED -> Pair(MaterialTheme.colorScheme.secondary, Icons.Default.Info)
        NotificationStatus.FAILED -> Pair(MaterialTheme.colorScheme.error, Icons.Default.Error)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )

            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.category.label} • ${item.timestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = statusColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = item.statusBadge.label,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
