package com.meshlink.ui.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class MeshAnimations(
    val fast: Int = 150,
    val normal: Int = 300,
    val slow: Int = 500,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f), // Standard Material 3 Easing
    val decelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
) {
    val standardTransition = tween<Float>(durationMillis = normal, easing = standardEasing)
    val fastTransition = tween<Float>(durationMillis = fast, easing = decelerateEasing)
}

val LocalMeshAnimations = staticCompositionLocalOf { MeshAnimations() }
