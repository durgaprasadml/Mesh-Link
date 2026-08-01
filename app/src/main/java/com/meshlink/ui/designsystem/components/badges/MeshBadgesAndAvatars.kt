package com.meshlink.ui.designsystem.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MeshTheme.colors.primary,
    contentColor: Color = Color.Black
) {
    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.pill)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = contentColor
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true
) {
    val color = if (isOnline) MeshTheme.colors.connected else MeshTheme.colors.disconnected
    MeshBadge(text = status, modifier = modifier, containerColor = color.copy(alpha = 0.2f), contentColor = color)
}

@Composable
fun MeshAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    isOnline: Boolean? = null
) {
    val initials = name.take(2).uppercase()

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = modifier
                .size(size)
                .clip(MeshTheme.shapes.circular)
                .background(MeshTheme.colors.primary.copy(alpha = 0.15f))
                .border(1.dp, MeshTheme.colors.primary, MeshTheme.shapes.circular),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = (size.value * 0.35f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MeshTheme.colors.primary
            )
        }
        if (isOnline != null) {
            val statusColor = if (isOnline) MeshTheme.colors.connected else MeshTheme.colors.disconnected
            Box(
                modifier = Modifier
                    .size((size.value * 0.28f).dp)
                    .clip(MeshTheme.shapes.circular)
                    .background(statusColor)
                    .border(1.5.dp, MeshTheme.colors.surface, MeshTheme.shapes.circular)
            )
        }
    }
}

@Composable
fun AvatarGroup(
    names: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-10).dp)
    ) {
        names.take(maxVisible).forEach { name ->
            MeshAvatar(name = name, size = 32.dp)
        }
        if (names.size > maxVisible) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MeshTheme.shapes.circular)
                    .background(MeshTheme.colors.surfaceVariant)
                    .border(1.dp, MeshTheme.colors.border, MeshTheme.shapes.circular),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${names.size - maxVisible}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun MeshChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.pill)
            .background(if (selected) MeshTheme.colors.primary else MeshTheme.colors.surfaceVariant)
            .border(
                0.5.dp,
                if (selected) MeshTheme.colors.primary else MeshTheme.colors.border,
                MeshTheme.shapes.pill
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.Black else MeshTheme.colors.textSecondary,
            letterSpacing = 0.5.sp
        )
    }
}
