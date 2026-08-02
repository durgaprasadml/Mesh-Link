package com.meshlink.ui.chat

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable animation specs and tactical motion transitions for Mesh-Link Chat UI.
 */
object ChatAnimations {

    // Spring Spec for Bubble entrance & expansion
    val BubbleSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val ReplySlideSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val PopSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SmoothFadeSpec: AnimationSpec<Float> = tween(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )

    val PulsingLoopSpec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    @Composable
    fun rememberPulseAlpha(): State<Float> {
        val transition = rememberInfiniteTransition(label = "pulseAlpha")
        return transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = PulsingLoopSpec,
            label = "pulseAlphaValue"
        )
    }

    @Composable
    fun rememberRadarPulseScale(): State<Float> {
        val transition = rememberInfiniteTransition(label = "radarPulse")
        return transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radarScale"
        )
    }
}
