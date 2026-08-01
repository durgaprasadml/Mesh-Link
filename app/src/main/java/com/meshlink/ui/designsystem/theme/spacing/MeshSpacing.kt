package com.meshlink.ui.designsystem.theme.spacing

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MeshSpacingScale(
    val n4: Dp = 4.dp,
    val n8: Dp = 8.dp,
    val n12: Dp = 12.dp,
    val n16: Dp = 16.dp,
    val n20: Dp = 20.dp,
    val n24: Dp = 24.dp,
    val n28: Dp = 28.dp,
    val n32: Dp = 32.dp,
    val n40: Dp = 40.dp,
    val n48: Dp = 48.dp,
    val n56: Dp = 56.dp,
    val n64: Dp = 64.dp
)

val LocalMeshSpacingScale = staticCompositionLocalOf { MeshSpacingScale() }
