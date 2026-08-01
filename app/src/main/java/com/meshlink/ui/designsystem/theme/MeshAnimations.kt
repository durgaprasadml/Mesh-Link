package com.meshlink.ui.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.meshlink.ui.designsystem.motion.meshPulse
import com.meshlink.ui.designsystem.theme.motion.MeshMotion


@Immutable
data class MeshAnimations(
    val fast: Int = 150,
    val normal: Int = 300,
    val slow: Int = 500,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val decelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
    val motion: MeshMotion = MeshMotion
) {
    val standardTransition = tween<Float>(durationMillis = normal, easing = standardEasing)
    val fastTransition = tween<Float>(durationMillis = fast, easing = decelerateEasing)
    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    val staggerDelayMs: Int = 25

    val meshPulse = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = 1800
            0.0f at 0
            0.6f at 900
            1.0f at 1800
        }
    )

    val fabEntrance = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

val LocalMeshAnimations = staticCompositionLocalOf { MeshAnimations() }

fun Modifier.meshPressScale(scale: Float = 0.97f): Modifier = this.graphicsLayer {
    scaleX = scale
    scaleY = scale
}

fun Modifier.meshPulseEffect(enabled: Boolean = true): Modifier = if (!enabled) this else this.meshPulse()

fun Modifier.meshShimmerEffect(): Modifier = this.graphicsLayer {
    alpha = 0.85f
}

@Composable
fun Modifier.scaleOnPress(targetScale: Float = 0.96f): Modifier = this.meshPressScale(targetScale)

