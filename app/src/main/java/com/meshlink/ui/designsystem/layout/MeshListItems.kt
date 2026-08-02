package com.meshlink.ui.designsystem.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.components.badges.UnreadBadge
import com.meshlink.ui.designsystem.theme.ComponentTokens
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Inset Divider for Mesh-Link Lists.
 */
@Composable
fun MeshDivider(
    modifier: Modifier = Modifier,
    startIndent: Boolean = true,
    color: Color = MeshTheme.colors.divider
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (startIndent) 72.dp else 0.dp),
        thickness = 0.5.dp,
        color = color
    )
}

/**
 * Standard List Row component with 48dp minimum touch target, leading icon/avatar, trailing content, and ripple.
 */
@Composable
fun MeshListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ComponentTokens.MinTouchTargetSize),
        color = Color.Transparent,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MeshTheme.customTypography.subtitle,
                    color = MeshTheme.colors.textPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MeshTheme.customTypography.caption,
                        color = MeshTheme.colors.textSecondary
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

/**
 * Settings Row Component.
 */
@Composable
fun MeshSettingRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailingControl: (@Composable () -> Unit)? = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MeshTheme.colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
) {
    MeshListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        leadingContent = icon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MeshTheme.shapes.small)
                        .background(MeshTheme.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = trailingControl
    )
}

/**
 * Chat Conversation Row Component.
 */
@Composable
fun MeshChatRow(
    title: String,
    lastMessage: String,
    timestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarName: String = title,
    unreadCount: Int = 0,
    isOnline: Boolean = false,
    isEncrypted: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MeshTheme.colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MeshTheme.customTypography.title,
                        color = MeshTheme.colors.textPrimary
                    )
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MeshTheme.colors.connected)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MeshTheme.customTypography.subtitle,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.textPrimary
                    )
                    Text(
                        text = timestamp,
                        style = MeshTheme.customTypography.timestamp,
                        color = MeshTheme.colors.textTertiary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastMessage,
                        style = MeshTheme.customTypography.messagePreview,
                        color = MeshTheme.colors.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        UnreadBadge(count = unreadCount)
                    }
                }
            }
        }
    }
}

/**
 * Nearby Device Row Component.
 */
@Composable
fun MeshDeviceRow(
    deviceName: String,
    statusText: String,
    rssiDbm: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    MeshListRow(
        title = deviceName,
        subtitle = statusText,
        onClick = onClick,
        modifier = modifier,
        leadingContent = icon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MeshTheme.shapes.small)
                        .background(MeshTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = {
            Text(
                text = "$rssiDbm dBm",
                style = MeshTheme.customTypography.signalMetrics,
                color = if (rssiDbm >= -70) MeshTheme.colors.connected else MeshTheme.colors.warning
            )
        }
    )
}

/**
 * Quick Action Row Component.
 */
@Composable
fun MeshActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionColor: Color = MeshTheme.colors.primary
) {
    MeshListRow(
        title = title,
        onClick = onClick,
        modifier = modifier,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(22.dp)
            )
        }
    )
}
