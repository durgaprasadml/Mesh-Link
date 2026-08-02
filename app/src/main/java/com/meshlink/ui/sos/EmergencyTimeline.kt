package com.meshlink.ui.sos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmergencyHistoryItem(
    val id: String,
    val title: String,
    val timestamp: String,
    val deliveryState: String,
    val devicesReached: Int,
    val isSuccess: Boolean,
    val group: String // "Today", "Yesterday", "Earlier"
)

/**
 * Grouped Emergency History timeline.
 * Sections: Today, Yesterday, Earlier.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmergencyTimeline(
    state: SosUiState,
    onLearnMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    val historyItems = remember(state.status, state.sosSent) {
        if (state.sosSent || state.status != SosStatus.SAFE) {
            listOf(
                EmergencyHistoryItem(
                    id = "alert_today_1",
                    title = "Emergency Distress Broadcast",
                    timestamp = currentTimeStr,
                    deliveryState = when (state.status) {
                        SosStatus.BROADCASTING -> "Broadcasting..."
                        SosStatus.DELIVERED -> "Delivered"
                        SosStatus.FAILED -> "Failed"
                        SosStatus.SAFE -> "Sent"
                    },
                    devicesReached = state.relaysReached,
                    isSuccess = state.status == SosStatus.DELIVERED,
                    group = "Today"
                )
            )
        } else {
            emptyList()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Emergency History",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (historyItems.isEmpty()) {
            EmergencyEmptyState(onLearnMore = onLearnMore)
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val grouped = historyItems.groupBy { it.group }
                    grouped.forEach { (groupHeader, items) ->
                        // Section Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = groupHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items.forEachIndexed { index, item ->
                            EmergencyHistoryRow(item = item)
                            if (index < items.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(start = 64.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyHistoryRow(
    item: EmergencyHistoryItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${item.title}. State: ${item.deliveryState}. ${item.devicesReached} devices reached. Time: ${item.timestamp}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Surface
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
                text = "${item.deliveryState} • ${item.devicesReached} devices reached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = item.timestamp,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
