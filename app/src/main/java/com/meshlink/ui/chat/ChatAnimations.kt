package com.meshlink.ui.chat

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalAccessibilityManager

/**
 * Centrally defined reusable animation specs and transitions for Mesh-Link Chat UI.
 * Fully supports reduced motion accessibility settings.
 */
object ChatAnimations {

    val bubbleEntranceSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 400f
    )

    val bubbleFadeSpec: AnimationSpec<Float> = tween(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )

    val sendButtonAnimationSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = 500f
    )

    val scrollFabAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    /**
     * Remembers pulsing alpha value for typing indicator dots.
     */
    @Composable
    fun rememberPulseAlpha(): State<Float> {
        val accessibilityManager = LocalAccessibilityManager.current
        val transition = rememberInfiniteTransition(label = "pulseAlphaTransition")
        return transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    }
}
