package com.meshlink.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.Chat
import com.meshlink.util.MeshIdNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatChatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun CommunicationsSection(
    chats: List<Chat>,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToNearby: () -> Unit,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val filteredChats = remember(searchQuery, chats) {
        if (searchQuery.isBlank()) chats
        else chats.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (filteredChats.isEmpty()) {
            RecentChatsEmptyState(
                searchQuery = searchQuery,
                onStartChatting = onNavigateToNearby
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                filteredChats.forEachIndexed { index, chat ->
                    RecentChatRow(
                        chat = chat,
                        onClick = {
                            val safeName = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) }
                            onNavigateToChat(chat.id, safeName)
                        }
                    )
                    if (index < filteredChats.size - 1) {
                        ChatDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun RecentChatRow(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) }
    val formattedTime = formatChatTimestamp(chat.lastMessageAt)
    val hasUnread = chat.unreadCount > 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Chat with $displayName, ${chat.lastMessage ?: ""}"
            },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 48dp Avatar with Online Dot
            ChatAvatar(
                name = displayName,
                avatarUri = chat.avatarUri,
                isOnline = true,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Contact Name & Last Message Preview with E2EE lock icon
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MeshLockBadge(modifier = Modifier.padding(end = 4.dp))
                    Text(
                        text = chat.lastMessage ?: "Tap to open conversation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timestamp & Unread Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                ChatTimestamp(
                    formattedTime = formattedTime,
                    hasUnread = hasUnread
                )

                if (hasUnread) {
                    Spacer(modifier = Modifier.height(4.dp))
                    UnreadBadge(count = chat.unreadCount)
                }
            }
        }
    }
}
