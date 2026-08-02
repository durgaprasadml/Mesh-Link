package com.meshlink.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmergencyTimeline(
    state: SosUiState,
    modifier: Modifier = Modifier
) {
    val currentTimeStr = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) }

    val timelineItems = remember(state.status, state.latitude, state.relaysReached) {
        listOf(
            EmergencyTimelineItem(
                id = "init",
                title = "Emergency Protocol Ready",
                description = "AES-256 encrypted distress channel standby",
                timestamp = currentTimeStr,
                isCompleted = true,
                isCurrent = state.status == SosStatus.SAFE,
                stage = EmergencyStage.SAFE
            ),
            EmergencyTimelineItem(
                id = "gps",
                title = "GPS Location Fix",
                description = if (state.latitude != null) "Coordinates acquired: ${String.format(Locale.US, "%.4f, %.4f", state.latitude, state.longitude)}" else "Acquiring GPS satellite fix...",
                timestamp = currentTimeStr,
                isCompleted = state.latitude != null,
                isCurrent = state.isFetchingLocation,
                stage = EmergencyStage.SAFE
            ),
            EmergencyTimelineItem(
                id = "broadcast",
                title = "Mesh Broadcast Initiated",
                description = "Distress signal emitted on BLE & Wi-Fi Direct channels",
                timestamp = currentTimeStr,
                isCompleted = state.status == SosStatus.BROADCASTING || state.status == SosStatus.DELIVERED,
                isCurrent = state.status == SosStatus.BROADCASTING,
                stage = EmergencyStage.BROADCASTING
            ),
            EmergencyTimelineItem(
                id = "relay",
                title = "Mesh Relays Reached",
                description = if (state.relaysReached > 0) "Relayed across ${state.relaysReached} peer nodes" else "Awaiting hop confirmation from nearby nodes...",
                timestamp = currentTimeStr,
                isCompleted = state.relaysReached > 0 || state.status == SosStatus.DELIVERED,
                isCurrent = state.status == SosStatus.BROADCASTING && state.relaysReached > 0,
                stage = EmergencyStage.BROADCASTING
            ),
            EmergencyTimelineItem(
                id = "delivered",
                title = "Emergency Delivered",
                description = if (state.status == SosStatus.DELIVERED) "Distress packet confirmed delivered to network" else "Awaiting final confirmation...",
                timestamp = currentTimeStr,
                isCompleted = state.status == SosStatus.DELIVERED,
                isCurrent = state.status == SosStatus.DELIVERED,
                stage = EmergencyStage.DELIVERED
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "MISSION TIMELINE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            timelineItems.forEachIndexed { index, item ->
                TimelineRow(
                    item = item,
                    isLast = index == timelineItems.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: EmergencyTimelineItem,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isCompleted -> MeshTheme.colors.success
                            item.isCurrent -> MeshTheme.colors.warning
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (item.isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(
                            if (item.isCompleted) MeshTheme.colors.success.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (item.isCurrent || item.isCompleted) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (item.isCompleted || item.isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
