package com.meshlink.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsTimeline(
    timelineEvents: List<TimelineEventUi> = defaultSampleTimelineEvents(),
    modifier: Modifier = Modifier
) {
    val events = timelineEvents.ifEmpty { defaultSampleTimelineEvents() }
    val grouped = events.groupBy { it.timeGroup }

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
                    text = "Activity Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chronological network connections, transfers & route updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            grouped.forEach { (group, eventList) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = when (group) {
                            TimelineTimeGroup.TODAY -> "Today"
                            TimelineTimeGroup.YESTERDAY -> "Yesterday"
                            TimelineTimeGroup.EARLIER -> "Earlier"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    eventList.forEach { evt ->
                        TimelineEventRow(event = evt)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventRow(event: TimelineEventUi) {
    val (icon, iconTint, containerColor) = when (event.type) {
        TimelineEventType.CONNECTED -> Triple(Icons.Default.BluetoothConnected, MeshTheme.colors.success, MeshTheme.colors.success.copy(alpha = 0.15f))
        TimelineEventType.DISCONNECTED -> Triple(Icons.Default.LinkOff, MeshTheme.colors.error, MaterialTheme.colorScheme.errorContainer)
        TimelineEventType.FILE_TRANSFER -> Triple(Icons.Default.CloudDownload, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        TimelineEventType.BROADCAST -> Triple(Icons.Default.Campaign, MeshTheme.colors.warning, MaterialTheme.colorScheme.secondaryContainer)
        TimelineEventType.ROUTE_CHANGE -> Triple(Icons.Default.Route, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        TimelineEventType.SYNC -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.surfaceContainerHighest)
    }

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(event.timestamp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun defaultSampleTimelineEvents(): List<TimelineEventUi> {
    val now = System.currentTimeMillis()
    return listOf(
        TimelineEventUi(
            id = "t1",
            timestamp = now - 1800000L,
            type = TimelineEventType.CONNECTED,
            title = "Peer Connected",
            description = "Node Pixel 8 connected via BLE Mesh",
            timeGroup = TimelineTimeGroup.TODAY
        ),
        TimelineEventUi(
            id = "t2",
            timestamp = now - 3600000L,
            type = TimelineEventType.FILE_TRANSFER,
            title = "File Transfer Completed",
            description = "Received dataset.zip (14.2 MB) from Relay-02",
            timeGroup = TimelineTimeGroup.TODAY
        ),
        TimelineEventUi(
            id = "t3",
            timestamp = now - 7200000L,
            type = TimelineEventType.ROUTE_CHANGE,
            title = "Route Updated",
            description = "Switched to 2-hop route via node_charlie",
            timeGroup = TimelineTimeGroup.TODAY
        ),
        TimelineEventUi(
            id = "t4",
            timestamp = now - 86400000L,
            type = TimelineEventType.BROADCAST,
            title = "Community Broadcast",
            description = "Sent emergency alert beacon to 6 active peers",
            timeGroup = TimelineTimeGroup.YESTERDAY
        ),
        TimelineEventUi(
            id = "t5",
            timestamp = now - 172800000L,
            type = TimelineEventType.SYNC,
            title = "Store & Forward Sync",
            description = "Synchronized 18 queued offline messages",
            timeGroup = TimelineTimeGroup.EARLIER
        )
    )
}
