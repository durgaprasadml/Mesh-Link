package com.meshlink.ui.designsystem.components.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
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
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Standard Material 3 Floating Action Button (56dp).
 */
@Composable
fun MeshFAB(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    containerColor: Color = MeshTheme.colors.primary,
    contentColor: Color = Color.Black
) {
    if (label != null) {
        MeshExtendedFAB(
            icon = icon,
            label = label,
            onClick = onClick,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.size(56.dp),
            shape = MeshTheme.shapes.fab,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Small Material 3 Floating Action Button (40dp).
 */
@Composable
fun MeshSmallFAB(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MeshTheme.colors.surfaceVariant,
    contentColor: Color = MeshTheme.colors.primary
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = MeshTheme.shapes.medium,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Extended Material 3 Floating Action Button (Icon + Label).
 */
@Composable
fun MeshExtendedFAB(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MeshTheme.colors.primary,
    contentColor: Color = Color.Black
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MeshTheme.shapes.fab,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Animated Material 3 Scroll-Aware FAB (Morphs between Compact and Extended).
 */
@Composable
fun MeshAnimatedFAB(
    icon: ImageVector,
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MeshTheme.colors.primary,
    contentColor: Color = Color.Black
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MeshTheme.shapes.fab,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (expanded) 20.dp else 16.dp,
                vertical = 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingDock(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.pill)
            .background(MeshTheme.colors.glassSurface)
            .border(0.5.dp, MeshTheme.colors.glassBorder, MeshTheme.shapes.pill)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun FloatingPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.floating)
            .background(MeshTheme.colors.surface)
            .border(0.5.dp, MeshTheme.colors.border, MeshTheme.shapes.floating)
            .padding(16.dp)
    ) {
        content()
    }
}

