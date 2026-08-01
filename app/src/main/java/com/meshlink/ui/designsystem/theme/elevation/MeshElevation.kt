package com.meshlink.ui.designsystem.theme.elevation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MeshElevationScale(
    val flat: Dp = 0.dp,
    val raised: Dp = 2.dp,
    val floating: Dp = 6.dp,
    val dialog: Dp = 12.dp,
    val overlay: Dp = 16.dp,
    val glass: Dp = 1.dp
)

val LocalMeshElevationScale = staticCompositionLocalOf { MeshElevationScale() }
