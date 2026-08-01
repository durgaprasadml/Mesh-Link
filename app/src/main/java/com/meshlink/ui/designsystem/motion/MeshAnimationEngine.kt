package com.meshlink.ui.designsystem.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.motion.MeshMotion

/**
 * Unified Animation Engine for Mesh-Link 2026.
 * Serves as the central API surface for Radar Ripples, Signal Pulses, Glow Effects, Breathing Widgets, and Motion Utilities.
 */
@Immutable
object MeshAnimationEngine {

    val Durations = MeshDurationPresets
    val Easings = MeshEasingPresets
    val Springs = MeshSpringPresets
    val Presets = MeshMotionPresets
    val Transitions = MeshTransitionSystem
    val Navigation = MeshNavigationMotion
}

@Composable
fun MeshRadarRipple(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00F59B),
    ringCount: Int = 3,
    size: Dp = 200.dp
) {
    val transition = rememberInfiniteTransition(label = "RadarRippleTransition")
    val animValues = (0 until ringCount).map { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = MeshMotion.DURATION_RADAR_SCAN, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RippleRing_$index"
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val maxRadius = this.size.minDimension / 2f
        animValues.forEach { anim ->
            val radius = maxRadius * anim.value
            val alpha = (1f - anim.value).coerceIn(0f, 1f) * 0.5f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun MeshSignalPulse(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    color: Color = Color(0xFF00F59B),
    content: @Composable () -> Unit
) {
    if (!active) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
        return
    }

    val transition = rememberInfiniteTransition(label = "SignalPulseTransition")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(MeshMotion.DURATION_SIGNAL_PULSE, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SignalPulseScale"
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MeshMotion.DURATION_SIGNAL_PULSE, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SignalPulseAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = (this.size.minDimension / 2f) * pulseScale
            drawCircle(
                color = color.copy(alpha = pulseAlpha),
                radius = radius
            )
        }
        content()
    }
}

fun Modifier.meshGlow(
    color: Color,
    radius: Dp = 16.dp,
    alpha: Float = 0.4f
): Modifier = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            radius.toPx(),
            0f,
            0f,
            shadowColor
        )
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            paint = paint
        )
    }
}
