package com.meshlink.ui.designsystem.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

/**
 * Production Glass Styles & Modifiers for Mesh Link 2026.
 */
@Composable
fun Modifier.meshLightGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp
): Modifier {
    val colors = LocalMeshSemanticColors.current
    return this
        .clip(shape)
        .background(colors.glassSurface)
        .border(borderWidth, colors.glassBorder, shape)
}

@Composable
fun Modifier.meshHeavyGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp
): Modifier {
    val colors = LocalMeshSemanticColors.current
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            colors.glassBorder.copy(alpha = 0.4f),
            colors.glassBorder.copy(alpha = 0.1f)
        )
    )
    return this
        .clip(shape)
        .background(colors.elevatedSurface.copy(alpha = 0.85f))
        .border(borderWidth, gradientBrush, shape)
}

@Composable
fun Modifier.meshNavigationGlass(
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier {
    val colors = LocalMeshSemanticColors.current
    return this
        .clip(shape)
        .background(colors.primaryBackground.copy(alpha = 0.88f))
        .border(1.dp, colors.border.copy(alpha = 0.4f), shape)
}

@Composable
fun Modifier.meshFloatingGlass(
    shape: Shape = RoundedCornerShape(32.dp)
): Modifier {
    val colors = LocalMeshSemanticColors.current
    val glowBrush = Brush.radialGradient(
        colors = listOf(colors.glowAccent.copy(alpha = 0.15f), Color.Transparent)
    )
    return this
        .clip(shape)
        .background(colors.cardSurface.copy(alpha = 0.90f))
        .background(glowBrush)
        .border(1.dp, colors.primary.copy(alpha = 0.3f), shape)
}

@Composable
fun Modifier.meshDialogGlass(
    shape: Shape = RoundedCornerShape(28.dp)
): Modifier {
    val colors = LocalMeshSemanticColors.current
    return this
        .clip(shape)
        .background(colors.elevatedSurface.copy(alpha = 0.95f))
        .border(1.5.dp, colors.primary.copy(alpha = 0.25f), shape)
}

@Composable
fun Modifier.meshHeroGlass(
    shape: Shape = RoundedCornerShape(32.dp)
): Modifier {
    val colors = LocalMeshSemanticColors.current
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.15f),
            colors.secondary.copy(alpha = 0.05f),
            colors.cardSurface.copy(alpha = 0.80f)
        )
    )
    return this
        .clip(shape)
        .background(heroBrush)
        .border(1.5.dp, colors.primary.copy(alpha = 0.4f), shape)
}
