package com.meshlink.ui.designsystem.theme.responsive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MeshWindowWidthClass {
    COMPACT, // < 600dp (standard phone portrait)
    MEDIUM,  // 600dp - 840dp (small tablet, foldable unfold)
    EXPANDED // > 840dp (large tablet, desktop)
}

enum class MeshWindowHeightClass {
    COMPACT, // < 480dp (phone landscape)
    MEDIUM,  // 480dp - 900dp (phone portrait, tablet)
    EXPANDED // > 900dp (large tablet portrait)
}

@Immutable
data class MeshWindowSize(
    val widthClass: MeshWindowWidthClass,
    val heightClass: MeshWindowHeightClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean
) {
    val isPortrait: Boolean get() = !isLandscape
    val isPhone: Boolean get() = widthClass == MeshWindowWidthClass.COMPACT
    val isFoldable: Boolean get() = widthClass == MeshWindowWidthClass.MEDIUM
    val isTablet: Boolean get() = widthClass == MeshWindowWidthClass.EXPANDED || (isLandscape && widthClass == MeshWindowWidthClass.MEDIUM)
}

val LocalMeshWindowSize = staticCompositionLocalOf {
    MeshWindowSize(
        widthClass = MeshWindowWidthClass.COMPACT,
        heightClass = MeshWindowHeightClass.MEDIUM,
        screenWidthDp = 360.dp,
        screenHeightDp = 800.dp,
        isLandscape = false
    )
}

@Composable
fun rememberMeshWindowSize(): MeshWindowSize {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        configuration.screenWidthDp < 600 -> MeshWindowWidthClass.COMPACT
        configuration.screenWidthDp < 840 -> MeshWindowWidthClass.MEDIUM
        else -> MeshWindowWidthClass.EXPANDED
    }

    val heightClass = when {
        configuration.screenHeightDp < 480 -> MeshWindowHeightClass.COMPACT
        configuration.screenHeightDp < 900 -> MeshWindowHeightClass.MEDIUM
        else -> MeshWindowHeightClass.EXPANDED
    }

    return MeshWindowSize(
        widthClass = widthClass,
        heightClass = heightClass,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp,
        isLandscape = isLandscape
    )
}
