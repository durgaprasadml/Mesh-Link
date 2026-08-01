package com.meshlink.ui.designsystem.theme.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MeshDeviceType {
    SMALL_PHONE,
    LARGE_PHONE,
    FOLDABLE,
    TABLET,
    DESKTOP_PREVIEW
}

enum class MeshOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class MeshNavigationLayout {
    BOTTOM_BAR,
    NAVIGATION_RAIL
}

@Immutable
data class MeshResponsiveLayoutConfig(
    val deviceType: MeshDeviceType = MeshDeviceType.LARGE_PHONE,
    val orientation: MeshOrientation = MeshOrientation.PORTRAIT,
    val navigationLayout: MeshNavigationLayout = MeshNavigationLayout.BOTTOM_BAR,
    val screenWidthDp: Dp = 360.dp,
    val screenHeightDp: Dp = 640.dp,
    val gridColumns: Int = 1,
    val horizontalPadding: Dp = 16.dp,
    val verticalPadding: Dp = 16.dp,
    val fabOffsetBottom: Dp = 16.dp
)

typealias MeshWindowSize = MeshResponsiveLayoutConfig

val LocalMeshWindowSize = staticCompositionLocalOf { MeshResponsiveLayoutConfig() }

@Composable
fun rememberMeshResponsiveConfig(): MeshResponsiveLayoutConfig {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val orientation = if (isLandscape) MeshOrientation.LANDSCAPE else MeshOrientation.PORTRAIT

    val deviceType = when {
        width < 360.dp -> MeshDeviceType.SMALL_PHONE
        width < 600.dp -> MeshDeviceType.LARGE_PHONE
        width < 840.dp -> MeshDeviceType.FOLDABLE
        width < 1200.dp -> MeshDeviceType.TABLET
        else -> MeshDeviceType.DESKTOP_PREVIEW
    }

    val navigationLayout = if (width >= 720.dp || isLandscape) {
        MeshNavigationLayout.NAVIGATION_RAIL
    } else {
        MeshNavigationLayout.BOTTOM_BAR
    }

    val gridColumns = when (deviceType) {
        MeshDeviceType.SMALL_PHONE -> 1
        MeshDeviceType.LARGE_PHONE -> if (isLandscape) 2 else 1
        MeshDeviceType.FOLDABLE -> 2
        MeshDeviceType.TABLET -> if (isLandscape) 3 else 2
        MeshDeviceType.DESKTOP_PREVIEW -> 4
    }

    val horizontalPadding = when (deviceType) {
        MeshDeviceType.SMALL_PHONE -> 12.dp
        MeshDeviceType.LARGE_PHONE -> 16.dp
        MeshDeviceType.FOLDABLE -> 24.dp
        MeshDeviceType.TABLET -> 32.dp
        MeshDeviceType.DESKTOP_PREVIEW -> 48.dp
    }

    val verticalPadding = when (deviceType) {
        MeshDeviceType.SMALL_PHONE -> 12.dp
        MeshDeviceType.LARGE_PHONE -> 16.dp
        else -> 24.dp
    }

    val fabOffsetBottom = if (navigationLayout == MeshNavigationLayout.BOTTOM_BAR) 16.dp else 24.dp

    return remember(width, height, isLandscape) {
        MeshResponsiveLayoutConfig(
            deviceType = deviceType,
            orientation = orientation,
            navigationLayout = navigationLayout,
            screenWidthDp = width,
            screenHeightDp = height,
            gridColumns = gridColumns,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            fabOffsetBottom = fabOffsetBottom
        )
    }
}

@Composable
fun rememberMeshWindowSize(): MeshWindowSize = rememberMeshResponsiveConfig()
