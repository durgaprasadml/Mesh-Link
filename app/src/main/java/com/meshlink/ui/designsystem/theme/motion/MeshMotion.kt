package com.meshlink.ui.designsystem.theme.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Material 3 Expressive & Motion tokens for Mesh Link 2026.
 */
@Immutable
data class MeshMotion(
    val fastDurationMs: Int = 150,
    val normalDurationMs: Int = 300,
    val slowDurationMs: Int = 500,

    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val decelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
    val accelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f),

    // Spring Specs
    val lowBounceSpring: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    val mediumBounceSpring: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    val highBounceSpring: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    ),

    // Transition Specs
    val containerTransformSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    val sharedAxisXSpec: AnimationSpec<Float> = tween(durationMillis = 300, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)),
    val sharedAxisYSpec: AnimationSpec<Float> = tween(durationMillis = 300, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)),
    val sharedAxisZSpec: AnimationSpec<Float> = tween(durationMillis = 300, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)),
    val fadeThroughSpec: AnimationSpec<Float> = tween(durationMillis = 250, easing = LinearEasing),
    val fadeScaleSpec: AnimationSpec<Float> = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
)

val LocalMeshMotion = staticCompositionLocalOf { MeshMotion() }

/** Reusable pulse opacity modifier for active mesh nodes & SOS rings. */
fun Modifier.meshPulseEffect(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    this.graphicsLayer { this.alpha = alpha }
}

/** Reusable shimmer modifier for loading skeletons. */
fun Modifier.meshShimmerEffect(showShimmer: Boolean = true, targetColor: Color = Color(0x3300F59B)): Modifier = composed {
    if (!showShimmer) return@composed this
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        targetColor.copy(alpha = 0.1f),
        targetColor.copy(alpha = 0.4f),
        targetColor.copy(alpha = 0.1f)
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 200f, translateAnim - 200f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

/** Micro-press scaling modifier for interactive elements. */
@Composable
fun Modifier.meshPressScale(targetScale: Float = 0.96f): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(targetScale, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
        } else {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
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
