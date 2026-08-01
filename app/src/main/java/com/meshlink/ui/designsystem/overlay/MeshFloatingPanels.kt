package com.meshlink.ui.designsystem.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Unified Floating Panels Framework for Mesh-Link 2026.
 * Includes Floating Panel base container, Context Menu, Tooltip,
 * and Floating Card with shared glassmorphism and spring motion.
 */

@Composable
fun MeshFloatingPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(MeshTheme.shapes.large)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MeshTheme.colors.glassBorder,
                            MeshTheme.colors.border.copy(alpha = 0.3f)
                        )
                    ),
                    shape = MeshTheme.shapes.large
                ),
            color = MeshTheme.colors.glassSurface,
            tonalElevation = MeshTheme.elevation.floating,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

/**
 * Item data model for Context Menu.
 */
data class MeshContextMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val showDividerAfter: Boolean = false
)

/**
 * Context Menu overlay panel.
 */
@Composable
fun MeshContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MeshContextMenuItem>,
    modifier: Modifier = Modifier
) {
    if (!expanded) return

    MeshFloatingPanel(
        visible = expanded,
        modifier = modifier.width(220.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, item ->
                val itemColor = if (item.isDestructive) MeshTheme.colors.error else MeshTheme.colors.textPrimary

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MeshTheme.shapes.small)
                        .clickable {
                            item.onClick()
                            onDismissRequest()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = itemColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = item.label,
                        fontSize = 14.sp,
                        fontWeight = if (item.isDestructive) FontWeight.SemiBold else FontWeight.Normal,
                        color = itemColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (item.showDividerAfter && index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MeshTheme.colors.border.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Tooltip overlay box with glass styling.
 */
@Composable
fun MeshTooltip(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f)
    ) {
        Surface(
            modifier = modifier
                .clip(MeshTheme.shapes.small)
                .border(
                    width = 0.5.dp,
                    color = MeshTheme.colors.glassBorder,
                    shape = MeshTheme.shapes.small
                ),
            color = MeshTheme.colors.surface,
            tonalElevation = MeshTheme.elevation.overlay,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MeshTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Floating Card overlay for active widgets, stream status, or node transfers.
 */
@Composable
fun MeshFloatingCard(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = Icons.Default.Info,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut(spring(stiffness = Spring.StiffnessHigh)) + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(MeshTheme.shapes.medium)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MeshTheme.colors.glassBorder,
                            MeshTheme.colors.border.copy(alpha = 0.2f)
                        )
                    ),
                    shape = MeshTheme.shapes.medium
                )
                .then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                ),
            color = MeshTheme.colors.surface,
            tonalElevation = MeshTheme.elevation.floating,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MeshTheme.colors.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MeshTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeshTheme.colors.textPrimary
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = MeshTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MeshTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (content != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}
