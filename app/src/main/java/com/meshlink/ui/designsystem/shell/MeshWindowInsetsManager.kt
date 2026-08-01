package com.meshlink.ui.designsystem.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

/**
 * Window Insets Manager Architecture for Mesh-Link 2026.
 * Centralized, single source of truth for all WindowInsets across the application shell layout.
 * Standardizes status bar, navigation bar, IME keyboard, display cutout, and safe content padding.
 */
@Immutable
data class MeshWindowInsetsHolder(
    val statusBarHeight: Dp = 0.dp,
    val navigationBarHeight: Dp = 0.dp,
    val imeHeight: Dp = 0.dp,
    val isImeVisible: Boolean = false,
    val cutoutLeft: Dp = 0.dp,
    val cutoutRight: Dp = 0.dp,
    val cutoutTop: Dp = 0.dp,
    val cutoutBottom: Dp = 0.dp,
    val systemBarsPadding: PaddingValues = PaddingValues(0.dp),
    val safeContentPadding: PaddingValues = PaddingValues(0.dp)
)

val LocalMeshWindowInsetsManager = staticCompositionLocalOf { MeshWindowInsetsHolder() }
val LocalMeshWindowInsets = staticCompositionLocalOf { MeshWindowInsetsHolder() }

@Composable
fun ProvideMeshWindowInsetsManager(
    content: @Composable () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    val imeInsets = WindowInsets.ime
    val imePadding = imeInsets.asPaddingValues()
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val systemBars = WindowInsets.systemBars.asPaddingValues()

    val imeBottom = imePadding.calculateBottomPadding()
    val isImeVisible = imeInsets.getBottom(density) > 0

    val holder = MeshWindowInsetsHolder(
        statusBarHeight = statusBars.calculateTopPadding(),
        navigationBarHeight = navigationBars.calculateBottomPadding(),
        imeHeight = imeBottom,
        isImeVisible = isImeVisible,
        cutoutLeft = cutout.calculateLeftPadding(layoutDirection),
        cutoutRight = cutout.calculateRightPadding(layoutDirection),
        cutoutTop = cutout.calculateTopPadding(),
        cutoutBottom = cutout.calculateBottomPadding(),
        systemBarsPadding = systemBars,
        safeContentPadding = PaddingValues(
            top = statusBars.calculateTopPadding(),
            bottom = maxOf(navigationBars.calculateBottomPadding(), imeBottom) + MeshSpacing.BottomNavHeight,
            start = systemBars.calculateLeftPadding(layoutDirection),
            end = systemBars.calculateRightPadding(layoutDirection)
        )
    )

    CompositionLocalProvider(
        LocalMeshWindowInsetsManager provides holder,
        LocalMeshWindowInsets provides holder,
        content = content
    )
}

@Composable
fun ProvideMeshWindowInsets(
    content: @Composable () -> Unit
) = ProvideMeshWindowInsetsManager(content)
