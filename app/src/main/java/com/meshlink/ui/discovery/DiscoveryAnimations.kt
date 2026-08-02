package com.meshlink.ui.discovery

import android.provider.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Reusable discovery animations with reduced motion support.
 */
object DiscoveryAnimations {

    // Spring Motion Specs
    val NodeSelectSpring = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 300f
    )

    val CardEntranceSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 250f
    )

    @Composable
    fun isReducedMotion(): Boolean {
        val context = LocalContext.current
        return remember(context) {
            try {
                val scale = Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                )
                scale == 0f
            } catch (e: Exception) {
                false
            }
        }
    }

    @Composable
    fun rememberRadarRotation(isScanning: Boolean): State<Float> {
        val reducedMotion = isReducedMotion()
        val transition = rememberInfiniteTransition(label = "RadarRotation")
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (reducedMotion) 8000 else if (isScanning) 3200 else 6000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "RadarDegrees"
        )
    }

    @Composable
    fun rememberRippleExpansion(): State<Float> {
        val reducedMotion = isReducedMotion()
        val transition = rememberInfiniteTransition(label = "RippleExpansion")
        return transition.animateFloat(
            initialValue = 0.1f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (reducedMotion) 4000 else 2400,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "RippleProgress"
        )
    }

    @Composable
    fun rememberNodePulseScale(): State<Float> {
        val reducedMotion = isReducedMotion()
        val transition = rememberInfiniteTransition(label = "NodePulse")
        return transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (reducedMotion) 3000 else 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseScale"
        )
    }

    @Composable
    fun rememberBeamAlpha(): State<Float> {
        val transition = rememberInfiniteTransition(label = "ConnectionBeam")
        return transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BeamAlpha"
        )
    }

    @Composable
    fun rememberScanBreathingAlpha(): State<Float> {
        val transition = rememberInfiniteTransition(label = "ScanBreathing")
        return transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ScanAlpha"
        )
    }
}
