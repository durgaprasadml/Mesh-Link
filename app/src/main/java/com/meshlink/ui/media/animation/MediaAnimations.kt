package com.meshlink.ui.media.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable motion physics and procedural animation brushes for Media UI.
 */
object MediaAnimations {

    val SpringFast = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SpringGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val FadeSpec = tween<Float>(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )

    /**
     * Animated shimmer brush for media loading placeholders and active transfer bars.
     */
    @Composable
    fun rememberShimmerBrush(
        targetValue: Float = 1000f,
        durationMillis: Int = 1200
    ): Brush {
        val infiniteTransition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShimmerTranslate"
        )

        val shimmerColors = listOf(
            MeshTheme.colors.surfaceVariant,
            MeshTheme.colors.primary.copy(alpha = 0.25f),
            MeshTheme.colors.surfaceVariant
        )

        return Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 300f, translateAnim - 300f),
            end = Offset(translateAnim, translateAnim)
        )
    }

    /**
     * Animated pulse glow alpha for active upload/download indicators.
     */
    @Composable
    fun rememberPulseAlpha(
        minAlpha: Float = 0.3f,
        maxAlpha: Float = 0.95f,
        durationMs: Int = 1000
    ): Float {
        val transition = rememberInfiniteTransition(label = "PulseAlphaTransition")
        val alpha by transition.animateFloat(
            initialValue = minAlpha,
            targetValue = maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )
        return alpha
    }

    /**
     * Animated scale transform for pinch/zoom spring feedback.
     */
    @Composable
    fun rememberWaveformHeights(
        barCount: Int = 24,
        isPlaying: Boolean = false
    ): List<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "WaveformTransition")
        val heights = mutableListOf<Float>()
        for (i in 0 until barCount) {
            val anim by infiniteTransition.animateFloat(
                initialValue = 0.2f + (i % 5) * 0.1f,
                targetValue = if (isPlaying) 0.95f else 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + (i % 7) * 80, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WaveformBar_$i"
            )
            heights.add(if (isPlaying) anim else 0.25f + (i % 4) * 0.1f)
        }
        return heights
    }
}

fun Modifier.mediaShimmerBackground(): Modifier = composed {
    val brush = MediaAnimations.rememberShimmerBrush()
    this.background(brush)
}
