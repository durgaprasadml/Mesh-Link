package com.meshlink.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics

/**
 * Tactical & Smooth Modern Animations for Home Screen.
 * Fully supports reduced motion and Material 3 motion specs.
 */

/**
 * Animated radar pulse state for node activity indicators.
 */
class MeshPulseState(
    val ring1Scale: State<Float>,
    val ring1Alpha: State<Float>,
    val ring2Scale: State<Float>,
    val ring2Alpha: State<Float>
)

@Composable
fun rememberMeshRadarPulse(): MeshPulseState {
    val transition = rememberInfiniteTransition(label = "RadarPulse")

    val ring1Scale = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ring1Scale"
    )

    val ring1Alpha = transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ring1Alpha"
    )

    val ring2Scale = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ring2Scale"
    )

    val ring2Alpha = transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ring2Alpha"
    )

    return remember(ring1Scale, ring1Alpha, ring2Scale, ring2Alpha) {
        MeshPulseState(ring1Scale, ring1Alpha, ring2Scale, ring2Alpha)
    }
}

/**
 * Staggered entrance animation modifier for smooth home section reveals.
 */
fun Modifier.homeSectionStagger(
    index: Int,
    baseDelayMs: Int = 60
): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        val totalDelay = index * baseDelayMs
        kotlinx.coroutines.delay(totalDelay.toLong())
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        val totalDelay = index * baseDelayMs
        kotlinx.coroutines.delay(totalDelay.toLong())
        offsetYAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    this.graphicsLayer {
        alpha = alphaAnim.value
        translationY = offsetYAnim.value
    }
}

/**
 * Smooth entrance animation for chat insertion.
 */
fun Modifier.chatRowInsertion(): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.95f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    this.graphicsLayer {
        alpha = alphaAnim.value
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
    }
}

/**
 * Empty state smooth fade and slide entrance animation modifier.
 */
fun Modifier.emptyStateFade(): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        offsetYAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    this.graphicsLayer {
        alpha = alphaAnim.value
        translationY = offsetYAnim.value
    }
}

/**
 * Tactile spring press feedback with haptics for home cards & actions.
 */
fun Modifier.tactileClick(
    onClick: () -> Unit,
    pressScale: Float = 0.96f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = rememberMeshHaptics()

    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptics.buttonPress()
            scaleAnim.animateTo(
                targetValue = pressScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
        } else {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    this
        .scale(scaleAnim.value)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
