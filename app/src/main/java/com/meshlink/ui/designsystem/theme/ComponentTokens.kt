package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central Component Tokens for Mesh-Link Design System.
 * Defines standard heights, radii, padding, touch targets, and avatar sizes.
 */
@Immutable
object ComponentTokens {
    // ── Touch Target & Heights ──
    val MinTouchTargetSize: Dp = 48.dp
    val ButtonHeightStandard: Dp = 48.dp
    val ButtonHeightCompact: Dp = 36.dp
    val ButtonHeightLarge: Dp = 56.dp

    // ── Search & Input Fields ──
    val SearchBarHeight: Dp = 48.dp
    val SearchBarCornerRadius: Dp = 28.dp
    val InputFieldMinHeight: Dp = 56.dp
    val InputCornerRadius: Dp = 12.dp

    // ── Navigation Components ──
    val NavigationBarHeight: Dp = 64.dp
    val NavigationRailWidth: Dp = 80.dp
    val TopAppBarHeight: Dp = 56.dp

    // ── Overlays & Floating Surface Radii ──
    val DialogCornerRadius: Dp = 28.dp
    val BottomSheetCornerRadius: Dp = 28.dp
    val CardCornerRadius: Dp = 16.dp
    val FABCornerRadius: Dp = 16.dp

    // ── FAB Dimensions ──
    val FABSizeStandard: Dp = 56.dp
    val FABSizeSmall: Dp = 40.dp
    val FABSizeLarge: Dp = 72.dp

    // ── Avatar Sizes ──
    val AvatarSizeSmall: Dp = 32.dp
    val AvatarSizeMedium: Dp = 40.dp
    val AvatarSizeLarge: Dp = 48.dp
    val AvatarSizeJumbo: Dp = 64.dp

    // ── Stroke Widths ──
    val StrokeWidthThin: Dp = 1.dp
    val StrokeWidthStandard: Dp = 1.5.dp
    val StrokeWidthThick: Dp = 2.dp
}
