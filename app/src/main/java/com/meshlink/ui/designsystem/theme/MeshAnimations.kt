package com.meshlink.ui.designsystem.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Immutable
data class MeshAnimations(
    val fast: Int = 150,
    val normal: Int = 300,
    val slow: Int = 500,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val decelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
) {
    val standardTransition = tween<Float>(durationMillis = normal, easing = standardEasing)
    val fastTransition = tween<Float>(durationMillis = fast, easing = decelerateEasing)
    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Used for staggered list item entrance — delay per index position. */
    val staggerDelayMs: Int = 25

    /**
     * Mesh scanning pulse animation spec — infinite repeating ring expand/fade.
     * Used in Nearby canvas radar rings and SOS activation rings.
     */
    val meshPulse = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = 1800
            0.0f at 0
            0.6f at 900
            1.0f at 1800
        }
    )

    /** FAB entrance spring — medium bouncy for a lively feel. */
    val fabEntrance = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

val LocalMeshAnimations = staticCompositionLocalOf { MeshAnimations() }

/**
 * Press-scale + alpha micro-animation for interactive cards and buttons.
 * Applies a spring-animated scale to 96% on press and alpha to 90%,
 * releasing back with a low-bounce spring for a premium tactile feel.
 */
@Composable
fun Modifier.scaleOnPress(targetScale: Float = 0.96f): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    androidx.compose.runtime.LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(targetScale, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            alpha.animateTo(0.90f, tween(durationMillis = 80))
        } else {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            alpha.animateTo(1f, tween(durationMillis = 120))
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}
