package com.meshlink.ui.designsystem.theme.elevation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 8-Tier Elevation Model for Mesh-Link 2026 Original Design System.
 */
@Immutable
data class MeshElevationScale(
    val flat: Dp = 0.dp,
    val raised: Dp = 2.dp,
    val level1: Dp = 2.dp,
    val floating: Dp = 4.dp,
    val level2: Dp = 4.dp,
    val hero: Dp = 6.dp,
    val overlay: Dp = 8.dp,
    val level3: Dp = 8.dp,
    val emergency: Dp = 12.dp,
    val level4: Dp = 12.dp,
    val level5: Dp = 16.dp,
    val glass: Dp = 0.dp,
    val surface: Dp = 0.dp,
    val card: Dp = 2.dp,
    val fab: Dp = 6.dp,
    val navigation: Dp = 4.dp,
    val bottomSheet: Dp = 8.dp,
    val dialog: Dp = 6.dp,
    val dropdown: Dp = 8.dp,
    val popup: Dp = 8.dp
)

typealias MeshElevation = MeshElevationScale
fun MeshElevation(): MeshElevationScale = MeshElevationScale()

val LocalMeshElevation = staticCompositionLocalOf { MeshElevationScale() }
