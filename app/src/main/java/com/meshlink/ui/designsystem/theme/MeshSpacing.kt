package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized Design System Spacing Tokens & Layout Rules for Mesh Link.
 */
object MeshSpacing {
    val XS: Dp = 4.dp
    val SM: Dp = 8.dp
    val MD: Dp = 12.dp
    val LG: Dp = 16.dp
    val XL: Dp = 20.dp
    val XXL: Dp = 24.dp
    val Section: Dp = 32.dp
    val Hero: Dp = 40.dp

    // Layout Rules Design System Tokens
    val ScreenPadding: Dp = 20.dp
    val TopSafeArea: Dp = 16.dp

    val TopAppBarHeight: Dp = 64.dp
    val TopAppBarHorizontalPadding: Dp = 20.dp
    val TopAppBarBottomSpacing: Dp = 8.dp

    val SearchBarHeight: Dp = 56.dp
    val SearchBarCornerRadius: Dp = 28.dp
    val SearchBarHorizontalPadding: Dp = 20.dp
    val SearchBarIconSize: Dp = 20.dp
    val SearchBarSpacing: Dp = 20.dp

    val SectionTitleTopSpacing: Dp = 24.dp
    val SectionTitleBottomSpacing: Dp = 12.dp

    val CardCornerRadius: Dp = 20.dp
    val CardInternalPadding: Dp = 18.dp
    val CardElevation: Dp = 2.dp
    val CardSpacing: Dp = 12.dp

    val DashboardCardWidth: Dp = 160.dp
    val DashboardCardSpacing: Dp = 16.dp

    val ListItemVerticalPadding: Dp = 12.dp
    val ListItemInternalPadding: Dp = 16.dp
    val ListBottomSpacing: Dp = 120.dp

    val FabEndPadding: Dp = 16.dp
    val FabBottomPadding: Dp = 16.dp

    val BottomNavHeight: Dp = 80.dp
    val BottomNavSelectedIndicator: Dp = 48.dp
    val BottomNavHorizontalPadding: Dp = 12.dp

    // Graph / Topology
    /** Standard size for mesh node icon containers in the topology view. */
    val NodeSize: Dp = 44.dp

    // Section Gaps
    /** Gap between major screen sections (larger than CardSpacing). */
    val SectionGap: Dp = 28.dp

    // List
    /** Minimum height for a standard list item row. */
    val ListItemHeight: Dp = 72.dp
}

@Immutable
data class MeshSpacingTokens(
    val extraSmall: Dp = 2.dp,
    val small: Dp = MeshSpacing.XS,
    val mediumSmall: Dp = MeshSpacing.SM,
    val medium: Dp = MeshSpacing.MD,
    val mediumLarge: Dp = MeshSpacing.LG,
    val large: Dp = MeshSpacing.XL,
    val extraLarge: Dp = MeshSpacing.XXL,
    val huge: Dp = MeshSpacing.Section,
    val extraHuge: Dp = MeshSpacing.Hero,
    val giant: Dp = 48.dp,
    val extraGiant: Dp = 64.dp
)

val LocalMeshSpacing = staticCompositionLocalOf { MeshSpacingTokens() }

