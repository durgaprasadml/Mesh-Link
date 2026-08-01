package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.spacing.MeshSpacingScale

object MeshSpacing {
    val XS: Dp = 4.dp
    val SM: Dp = 8.dp
    val MD: Dp = 12.dp
    val LG: Dp = 16.dp
    val XL: Dp = 20.dp
    val XXL: Dp = 24.dp
    val SectionSM: Dp = 28.dp
    val Section: Dp = 32.dp
    val Hero: Dp = 40.dp
    val Giant: Dp = 48.dp
    val ExtraGiant: Dp = 56.dp
    val Mega: Dp = 64.dp

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
    val ListBottomSpacing: Dp = 24.dp

    val FabEndPadding: Dp = 16.dp
    val FabBottomPadding: Dp = 16.dp

    val BottomNavHeight: Dp = 80.dp
    val BottomNavSelectedIndicator: Dp = 48.dp
    val BottomNavHorizontalPadding: Dp = 12.dp

    val NodeSize: Dp = 44.dp
    val SectionGap: Dp = 28.dp
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
    val sectionSmall: Dp = MeshSpacing.SectionSM,
    val huge: Dp = MeshSpacing.Section,
    val extraHuge: Dp = MeshSpacing.Hero,
    val giant: Dp = MeshSpacing.Giant,
    val extraGiant: Dp = MeshSpacing.ExtraGiant,
    val mega: Dp = MeshSpacing.Mega,
    val scale: MeshSpacingScale = MeshSpacingScale()
)

val LocalMeshSpacing = staticCompositionLocalOf { MeshSpacingTokens() }
