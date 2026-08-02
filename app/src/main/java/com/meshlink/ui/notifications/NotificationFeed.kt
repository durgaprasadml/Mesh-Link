package com.meshlink.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun NotificationFeedSection(
    notifications: List<NotificationItemUi>,
    onNotificationClick: (NotificationItemUi) -> Unit,
    onQuickActionClick: (NotificationItemUi, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return

    val todayItems = notifications.filter { it.timestamp.contains("Today", ignoreCase = true) || it.timestamp.contains("min", ignoreCase = true) || it.timestamp.contains("now", ignoreCase = true) }
    val yesterdayItems = notifications.filter { it.timestamp.contains("Yesterday", ignoreCase = true) }
    val earlierItems = notifications.filter { it !in todayItems && it !in yesterdayItems }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (todayItems.isNotEmpty()) {
            NotificationGroupHeader(title = "Today")
            todayItems.forEach { item ->
                NotificationItemCard(
                    item = item,
                    onClick = { onNotificationClick(item) },
                    onAction = { action -> onQuickActionClick(item, action) }
                )
            }
        }

        if (yesterdayItems.isNotEmpty()) {
            NotificationGroupHeader(title = "Yesterday")
            yesterdayItems.forEach { item ->
                NotificationItemCard(
                    item = item,
                    onClick = { onNotificationClick(item) },
                    onAction = { action -> onQuickActionClick(item, action) }
                )
            }
        }

        if (earlierItems.isNotEmpty()) {
            NotificationGroupHeader(title = "Earlier")
            earlierItems.forEach { item ->
                NotificationItemCard(
                    item = item,
                    onClick = { onNotificationClick(item) },
                    onAction = { action -> onQuickActionClick(item, action) }
                )
            }
        }
    }
}

@Composable
fun NotificationGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun NotificationItemCard(
    item: NotificationItemUi,
    onClick: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon = when (item.category) {
        NotificationCategory.MESSAGES -> Icons.Default.Message
        NotificationCategory.BROADCASTS -> Icons.Default.Radio
        NotificationCategory.NEARBY_DEVICES -> Icons.Default.Person
        NotificationCategory.SOS -> Icons.Default.Warning
        NotificationCategory.TRANSFERS -> Icons.Default.Download
        NotificationCategory.SECURITY -> Icons.Default.Security
        else -> Icons.Default.Notifications
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (item.isRead) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isRead) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = if (item.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.actionLabel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        AssistChip(
                            onClick = { onAction(item.actionLabel) },
                            label = { Text(item.actionLabel, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }

            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
