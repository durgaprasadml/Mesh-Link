package com.meshlink.ui.designsystem.theme.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class MeshShapeScale(
    val small: Shape = RoundedCornerShape(4.dp),
    val mediumSmall: Shape = RoundedCornerShape(8.dp),
    val medium: Shape = RoundedCornerShape(12.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val extraLarge: Shape = RoundedCornerShape(24.dp),
    val jumbo: Shape = RoundedCornerShape(32.dp),
    val pill: Shape = RoundedCornerShape(50),
    val circle: Shape = CircleShape
)

val LocalMeshShapeScale = staticCompositionLocalOf { MeshShapeScale() }
