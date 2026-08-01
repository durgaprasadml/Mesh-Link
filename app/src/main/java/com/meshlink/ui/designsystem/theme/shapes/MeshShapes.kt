package com.meshlink.ui.designsystem.theme.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape System Tokens for Mesh-Link 2026 Original Design System.
 */
@Immutable
data class MeshShapeScale(
    val tiny: Shape = RoundedCornerShape(4.dp),
    val extraSmall: Shape = RoundedCornerShape(4.dp),
    val small: Shape = RoundedCornerShape(8.dp),
    val mediumSmall: Shape = RoundedCornerShape(10.dp),
    val medium: Shape = RoundedCornerShape(12.dp),
    val mediumLarge: Shape = RoundedCornerShape(20.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val extraLarge: Shape = RoundedCornerShape(24.dp),
    val xl: Shape = RoundedCornerShape(24.dp),
    val jumbo: Shape = RoundedCornerShape(32.dp),
    val pill: Shape = CircleShape,
    val circular: Shape = CircleShape,
    val floating: Shape = RoundedCornerShape(18.dp),
    val glass: Shape = RoundedCornerShape(16.dp),
    val navigation: Shape = RoundedCornerShape(24.dp),
    val fab: Shape = RoundedCornerShape(16.dp),
    val dialogs: Shape = RoundedCornerShape(24.dp),
    val bottomSheets: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val cards: Shape = RoundedCornerShape(16.dp),
    val buttons: Shape = RoundedCornerShape(12.dp),
    val inputs: Shape = RoundedCornerShape(12.dp)
)

typealias MeshShapes = MeshShapeScale
fun MeshShapes(): MeshShapeScale = MeshShapeScale()

val LocalMeshShapes = staticCompositionLocalOf { MeshShapeScale() }
