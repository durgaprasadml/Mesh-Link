package com.meshlink.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.meshlink.domain.model.Chat
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatRowItem(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val formattedTime = remember(chat.lastMessageAt) {
        com.meshlink.ui.util.DateTimeUtils.formatChatRowDate(chat.lastMessageAt)
    }
    val displayName = remember(chat.name, chat.id) {
        chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) }
    }
    val displayInitial = remember(chat.name) {
        chat.name.firstOrNull()?.toString()?.uppercase() ?: "?"
    }

    val semanticDescription = remember(displayName, chat.lastMessage, formattedTime, chat.unreadCount) {
        buildString {
            append("Chat with $displayName. ")
            if (!chat.lastMessage.isNullOrBlank()) {
                append("Last message: ${chat.lastMessage}. ")
            }
            if (formattedTime.isNotBlank()) {
                append("At $formattedTime. ")
            }
            if (chat.unreadCount > 0) {
                append("${chat.unreadCount} unread messages.")
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = semanticDescription
            }
            .padding(
                horizontal = MeshTheme.spacing.mediumLarge,
                vertical = MeshTheme.spacing.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        com.meshlink.ui.components.UserAvatarImage(
            avatarUri = chat.avatarUri,
            displayName = displayName,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))

        // Name & Last Message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Text(
                text = chat.lastMessage ?: "No messages yet",
                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))

        // Timestamp & Unread Badge
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formattedTime,
                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Box(
                    modifier = Modifier
                        .size(MeshTheme.spacing.extraLarge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
