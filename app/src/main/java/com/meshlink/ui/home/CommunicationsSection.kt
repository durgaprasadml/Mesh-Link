package com.meshlink.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.Chat
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.designsystem.theme.MeshTheme
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ACTIVE COMMUNICATIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MeshTheme.colors.textPrimary,
                    letterSpacing = 1.sp
                )
            }

            if (filteredChats.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = MeshTheme.colors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${filteredChats.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MeshTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (filteredChats.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MeshTheme.colors.border, RoundedCornerShape(20.dp)),
                color = MeshTheme.colors.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = if (searchQuery.isNotBlank()) "No Matching Communications" else "No Active Conversations",
                        description = if (searchQuery.isNotBlank()) {
                            "No mesh peer or conversation matched \"$searchQuery\"."
                        } else {
                            "Discover nearby nodes on BLE & Wi-Fi Direct to start secure multi-hop messaging."
                        },
                        primaryButtonText = if (searchQuery.isBlank()) "Find Nearby Nodes" else null,
                        onPrimaryButtonClick = if (searchQuery.isBlank()) onNavigateToNearby else null
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredChats.forEach { chat ->
                    TacticalChatCard(
                        chat = chat,
                        onClick = {
                            val safeName = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) }
                            onNavigateToChat(chat.id, safeName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalChatCard(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = chat.name.ifBlank { MeshIdNormalizer.canonicalize(chat.id) }
    val formattedTime = formatChatTimestamp(chat.lastMessageAt)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 0.5.dp,
                color = if (chat.unreadCount > 0) MeshTheme.colors.primary.copy(alpha = 0.5f) else MeshTheme.colors.border,
                shape = RoundedCornerShape(16.dp)
            )
            .tactileClick(onClick = onClick, pressScale = 0.98f),
        color = if (chat.unreadCount > 0) MeshTheme.colors.primary.copy(alpha = 0.04f) else MeshTheme.colors.surface,
        tonalElevation = MeshTheme.elevation.flat,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Mesh Node Ring Indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                com.meshlink.ui.components.UserAvatar(
                    avatarUri = chat.avatarUri,
                    name = displayName,
                    size = 48.dp
                )
                // Active mesh connection status dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                        .border(2.dp, MeshTheme.colors.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main chat summary info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Black else FontWeight.Bold,
                        color = MeshTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (chat.unreadCount > 0) MeshTheme.colors.primary else MeshTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message text with E2E lock icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2E Encrypted",
                            tint = MeshTheme.colors.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chat.lastMessage ?: "Secure mesh channel ready",
                            fontSize = 13.sp,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (chat.unreadCount > 0) MeshTheme.colors.textPrimary else MeshTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MeshTheme.colors.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${chat.unreadCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
