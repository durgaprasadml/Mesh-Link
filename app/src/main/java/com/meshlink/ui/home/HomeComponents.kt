package com.meshlink.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.components.UserAvatarImage

/**
 * Reusable presentation components for Mesh-Link Home Screen.
 * Strictly Material 3 compliant and presentation-only.
 */

/**
 * 48dp Chat Avatar with Online Mesh Dot Indicator.
 */
@Composable
fun ChatAvatar(
    name: String,
    identity: UserIdentity? = null,
    avatarUri: String? = null,
    isOnline: Boolean = true,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val firstLetter = name.trim().take(1).uppercase().ifEmpty { "?" }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            identity != null -> {
                UserAvatar(
                    identity = identity,
                    size = size
                )
            }
            !avatarUri.isNullOrBlank() -> {
                UserAvatarImage(
                    avatarUri = avatarUri,
                    displayName = name,
                    size = size
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = firstLetter,
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (isOnline) {
            OnlineDot(
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Online Mesh Indicator Dot.
 */
@Composable
fun OnlineDot(
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    dotColor: Color = Color(0xFF4CAF50)
) {
    Box(
        modifier = modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}

/**
 * Formatted Chat Timestamp Label.
 */
@Composable
fun ChatTimestamp(
    formattedTime: String,
    hasUnread: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = formattedTime,
        fontSize = 12.sp,
        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Green Pill Unread Messages Badge.
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFF2E7D32)
) {
    if (count <= 0) return

    val badgeText = if (count > 99) "99+" else "$count"

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(badgeColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Small E2EE Mesh Lock Icon Badge.
 */
@Composable
fun MeshLockBadge(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Encrypted Mesh Channel",
        tint = tint,
        modifier = modifier.size(12.dp)
    )
}

/**
 * Standard WhatsApp-style Chat Inset Divider.
 */
@Composable
fun ChatDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 76.dp,
    endIndent: Dp = 16.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent, end = endIndent),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

/**
 * M3 Section Header for Recent Chats or Quick Actions.
 */
@Composable
fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Quick Action Card Item Model.
 */
data class QuickActionCardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

/**
 * Production 96dp Quick Action Card.
 */
@Composable
fun QuickActionCard(
    item: QuickActionCardItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = item.onClick)
            .semantics {
                role = Role.Button
                contentDescription = "${item.title}, ${item.subtitle}"
            },
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Icon in 28dp circle container
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(item.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Title & Subtitle
            Column {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact Centered Empty State for Recent Chats.
 */
@Composable
fun RecentChatsEmptyState(
    searchQuery: String,
    onStartChatting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (searchQuery.isNotBlank()) "No matching conversations" else "No conversations yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (searchQuery.isNotBlank()) {
                "No chat matches \"$searchQuery\""
            } else {
                "Connect with nearby devices over BLE & Wi-Fi Direct to start messaging."
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartChatting,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                ),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Start chatting",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
