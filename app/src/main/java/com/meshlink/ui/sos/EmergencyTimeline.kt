package com.meshlink.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmergencyAlertRowItem(
    val id: String,
    val title: String,
    val timestamp: String,
    val deliveryState: String,
    val isSuccess: Boolean
)

@Composable
fun EmergencyTimeline(
    state: SosUiState,
    modifier: Modifier = Modifier
) {
    val currentTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    val alerts = remember(state.status, state.sosSent) {
        if (state.sosSent || state.status != SosStatus.SAFE) {
            listOf(
                EmergencyAlertRowItem(
                    id = "alert_1",
                    title = "Emergency Distress Broadcast",
                    timestamp = currentTimeStr,
                    deliveryState = when (state.status) {
                        SosStatus.BROADCASTING -> "Broadcasting"
                        SosStatus.DELIVERED -> "Delivered (${state.relaysReached} Hops)"
                        SosStatus.FAILED -> "Failed"
                        SosStatus.SAFE -> "Sent"
                    },
                    isSuccess = state.status == SosStatus.DELIVERED
                )
            )
        } else {
            emptyList()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Recent Emergency History",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (alerts.isEmpty()) {
            // Compact Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No emergency history",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your previous emergency broadcasts will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            alerts.forEachIndexed { index, item ->
                EmergencyAlertRow(item = item)
                if (index < alerts.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyAlertRow(
    item: EmergencyAlertRowItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Circle
        Surface(
            shape = CircleShape,
            color = if (item.isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (item.isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Radar,
                    contentDescription = null,
                    tint = if (item.isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.deliveryState,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = item.timestamp,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

