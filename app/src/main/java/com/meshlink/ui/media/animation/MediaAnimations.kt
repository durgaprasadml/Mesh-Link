package com.meshlink.ui.media.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable motion physics, animations, and procedural brushes for Media & File Sharing UI.
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

    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
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
     * Upload bounce vertical offset calculation.
     */
    @Composable
    fun rememberUploadBounceOffset(isUploading: Boolean = true): Float {
        if (!isUploading) return 0f
        val transition = rememberInfiniteTransition(label = "UploadBounceTransition")
        val offset by transition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "UploadBounceOffset"
        )
        return offset
    }

    /**
     * Download pulse scale calculation.
     */
    @Composable
    fun rememberDownloadPulseScale(isDownloading: Boolean = true): Float {
        if (!isDownloading) return 1f
        val transition = rememberInfiniteTransition(label = "DownloadPulseTransition")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DownloadPulseScale"
        )
        return scale
    }

    /**
     * Animated success tick scale transform.
     */
    @Composable
    fun rememberSuccessScale(trigger: Boolean): Float {
        val scale by animateFloatAsState(
            targetValue = if (trigger) 1f else 0f,
            animationSpec = SpringBouncy,
            label = "SuccessScale"
        )
        return scale
    }

    /**
     * Animated failure shake horizontal translation calculation.
     */
    @Composable
    fun rememberFailureShakeOffset(isFailed: Boolean): Float {
        if (!isFailed) return 0f
        val transition = rememberInfiniteTransition(label = "FailureShakeTransition")
        val offset by transition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "FailureShakeOffset"
        )
        return offset
    }

    /**
     * Waveform height array generator for dynamic voice notes and audio playback.
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

