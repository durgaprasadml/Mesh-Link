package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MeshSpacing(
    val extraSmall: Dp = 2.dp,
    val small: Dp = 4.dp,
    val mediumSmall: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val mediumLarge: Dp = 16.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val extraHuge: Dp = 40.dp,
    val giant: Dp = 48.dp,
    val extraGiant: Dp = 64.dp
)

val LocalMeshSpacing = staticCompositionLocalOf { MeshSpacing() }
