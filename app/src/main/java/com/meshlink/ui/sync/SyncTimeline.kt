package com.meshlink.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * SyncTimeline — Activity timeline displaying key sync events in chronological order.
 */
@Composable
fun SyncTimelineCard(
    timelineEvents: List<SyncTimelineUi>,
    modifier: Modifier = Modifier
) {
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
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Sync Timeline",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sync Activity Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${timelineEvents.size} Events",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (timelineEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No timeline events recorded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = timelineEvents,
                        key = { it.id }
                    ) { event ->
                        TimelineRowItem(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRowItem(
    event: SyncTimelineUi,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (event.eventType) {
        "Message Queued" -> Icons.Default.MoveToInbox to Color(0xFFFF9800)
        "Peer Connected" -> Icons.Default.BluetoothConnected to Color(0xFF2196F3)
        "Route Established" -> Icons.AutoMirrored.Filled.AltRoute to Color(0xFF9C27B0)
        "Delivered" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        "Retry" -> Icons.Default.Replay to Color(0xFFFF5722)
        "Sync Complete" -> Icons.Default.DoneAll to Color(0xFF00F59B)
        else -> Icons.Default.Info to MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(MeshTheme.shapes.tiny)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = event.eventType,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
