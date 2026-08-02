package com.meshlink.ui.security

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

object SecurityAnimations {

    /**
     * Infinite pulsing scale for encryption status icons/shields.
     */
    @Composable
    fun RememberEncryptionPulseScale(enabled: Boolean = true): Float {
        if (!enabled) return 1f
        val transition = rememberInfiniteTransition(label = "EncryptionPulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseScale"
        )
        return scale
    }

    /**
     * Infinite pulsing alpha glow for trusted status elements.
     */
    @Composable
    fun RememberTrustGlowAlpha(enabled: Boolean = true): Float {
        if (!enabled) return 0.6f
        val transition = rememberInfiniteTransition(label = "TrustGlow")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
        return alpha
    }

    /**
     * Horizontal shake offset animation for warnings or authentication failures.
     */
    @Composable
    fun RememberWarningShakeOffset(triggerShake: Boolean): IntOffset {
        val animatable = remember { Animatable(0f) }
        androidx.compose.runtime.LaunchedEffect(triggerShake) {
            if (triggerShake) {
                repeat(4) {
                    animatable.animateTo(12f, animationSpec = tween(50))
                    animatable.animateTo(-12f, animationSpec = tween(50))
                }
                animatable.animateTo(0f, animationSpec = tween(50))
            }
        }
        return IntOffset(x = animatable.value.toInt(), y = 0)
    }

    /**
     * Expanding connection ripple canvas effect for secure session establishment.
     */
    @Composable
    fun SecureConnectionRipple(
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colorScheme.primary,
        maxRadius: Dp = 60.dp
    ) {
        val transition = rememberInfiniteTransition(label = "ConnectionRipple")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RippleProgress"
        )

        Canvas(modifier = modifier.size(maxRadius * 2)) {
            val currentRadius = size.minDimension / 2f * progress
            val alpha = (1f - progress).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = alpha * 0.5f),
                radius = currentRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }

    /**
     * Shield pulse container wrapper.
     */
    @Composable
    fun ShieldPulseContainer(
        modifier: Modifier = Modifier,
        pulseColor: Color = MaterialTheme.colorScheme.primary,
        content: @Composable () -> Unit
    ) {
        val scale = RememberEncryptionPulseScale()
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            SecureConnectionRipple(
                modifier = Modifier.scale(scale),
                color = pulseColor
            )
            Box(modifier = Modifier.scale(scale)) {
                content()
            }
        }
    }
}
