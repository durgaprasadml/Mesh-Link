package com.meshlink.ui.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized UI Layout Constants for Mesh Link.
 * Delegated directly to MeshSpacing single-source-of-truth tokens.
 */
object LayoutConstants {
    val BottomBarHeight: Dp get() = MeshSpacing.BottomNavHeight
    val TopAppBarHeight: Dp get() = MeshSpacing.TopAppBarHeight
    val FabSize: Dp get() = 56.dp
    val FabBottomMargin: Dp get() = 16.dp
    val FabEndMargin: Dp get() = MeshSpacing.FabEndPadding
    
    val ScreenHorizontalPadding: Dp get() = 16.dp
    val ScreenVerticalPadding: Dp get() = MeshSpacing.ScreenPadding
    
    val CardSpacing: Dp get() = MeshSpacing.CardSpacing
    val HeaderSpacing: Dp get() = MeshSpacing.SearchBarSpacing
    val SectionSpacing: Dp get() = MeshSpacing.SectionTitleTopSpacing
    
    val CardCornerRadius: Dp get() = MeshSpacing.CardCornerRadius
    val CardInternalPadding: Dp get() = MeshSpacing.CardInternalPadding
    val CardElevation: Dp get() = MeshSpacing.CardElevation
    val ButtonCornerRadius: Dp get() = 12.dp
}

