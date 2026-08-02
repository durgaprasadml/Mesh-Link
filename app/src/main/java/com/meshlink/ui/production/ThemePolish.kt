package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Theme & Visual Consistency Utilities for Mesh-Link Phase 15.
 * Provides AMOLED true dark mode support, glassmorphic backdrop extensions,
 * and token enforcement for Mesh Design System.
 */

@Immutable
object MeshThemePolish {

    // Pure AMOLED Dark Colors
    val AmoledBackground = Color(0xFF000000)
    val AmoledSurface = Color(0xFF0D0D0D)
    val AmoledSurfaceVariant = Color(0xFF161616)

    // Standardized Glassmorphism Modifier
    fun Modifier.meshGlassSurface(
        backgroundColor: Color = Color(0x1AFFFFFF),
        borderColor: Color = Color(0x33FFFFFF),
        cornerRadius: Dp = 16.dp
    ): Modifier = this
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}

@Composable
fun MeshAmoledThemeContainer(
    isAmoledEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    if (isAmoledEnabled) {
        Box(
            modifier = Modifier
                .background(MeshThemePolish.AmoledBackground)
        ) {
            content()
        }
    } else {
        content()
    }
}
