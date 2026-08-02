package com.meshlink.ui.analytics

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

@Composable
fun rememberReducedMotionState(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            durationScale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun rememberAnimatedCounter(
    targetValue: Int,
    durationMs: Int = 800
): State<Int> {
    val isReduced = rememberReducedMotionState()
    if (isReduced) {
        return rememberUpdatedState(targetValue)
    }

    val animatable = remember { Animatable(targetValue.toFloat()) }
    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
        )
    }
    return remember {
        derivedStateOf { animatable.value.toInt() }
    }
}

@Composable
fun rememberAnimatedFloatCounter(
    targetValue: Float,
    durationMs: Int = 800
): State<Float> {
    val isReduced = rememberReducedMotionState()
    if (isReduced) {
        return rememberUpdatedState(targetValue)
    }

    val animatable = remember { Animatable(targetValue) }
    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
        )
    }
    return animatable.asState()
}

@Composable
fun rememberPulseAnimation(
    durationMs: Int = 1600
): State<Float> {
    val isReduced = rememberReducedMotionState()
    if (isReduced) return rememberUpdatedState(1.0f)

    val transition = rememberInfiniteTransition(label = "pulse_transition")
    return transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
}

@Composable
fun rememberBeamAnimation(
    durationMs: Int = 2400
): State<Float> {
    val isReduced = rememberReducedMotionState()
    if (isReduced) return rememberUpdatedState(0.5f)

    val transition = rememberInfiniteTransition(label = "beam_transition")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beam_progress"
    )
}

