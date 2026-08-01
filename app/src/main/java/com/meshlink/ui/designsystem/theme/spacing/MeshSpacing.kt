package com.meshlink.ui.designsystem.theme.spacing

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified 4dp Spacing Tokens for Mesh-Link 2026 Original Design System.
 */
@Immutable
data class MeshSpacingScale(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val extraSmall: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val small: Dp = 8.dp,
    val mediumSmall: Dp = 10.dp,
    val md: Dp = 12.dp,
    val medium: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val large: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val mediumLarge: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val extraLarge: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val section: Dp = 40.dp,
    val hero: Dp = 48.dp,
    val huge: Dp = 48.dp,
    val space64: Dp = 64.dp,
    val giant: Dp = 64.dp,
    val space80: Dp = 80.dp,
    val extraGiant: Dp = 80.dp,
    val space96: Dp = 96.dp,
    // Contextual Layout Tokens
    val screenInsetsHorizontal: Dp = 16.dp,
    val screenInsetsVertical: Dp = 16.dp,
    val sectionSpacing: Dp = 24.dp,
    val cardPadding: Dp = 16.dp,
    val listSpacing: Dp = 8.dp,
    val dialogPadding: Dp = 24.dp,
    val bottomSheetPadding: Dp = 20.dp,
    val fabBottomOffset: Dp = 16.dp,
    val navigationBarHeight: Dp = 64.dp,
    val navigationRailWidth: Dp = 80.dp,
    val topAppBarHeight: Dp = 56.dp
)

typealias MeshSpacingTokens = MeshSpacingScale
fun MeshSpacingTokens(): MeshSpacingScale = MeshSpacingScale()

val LocalMeshSpacing = staticCompositionLocalOf { MeshSpacingScale() }
