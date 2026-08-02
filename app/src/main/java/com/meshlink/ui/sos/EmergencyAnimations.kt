package com.meshlink.ui.sos

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable motion animations for the SOS Emergency Experience.
 * Reuses Phase 3 Motion Engine guidelines with lightweight Compose graphics.
 */

@Composable
fun EmergencyBeaconPulse(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    color: Color = MeshTheme.colors.danger,
    enabled: Boolean = true
) {
    if (!enabled) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "emergency_beacon_transition")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_ring1_scale"
    )

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_ring1_alpha"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_ring2_scale"
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 600, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_ring2_alpha"
    )

    Box(modifier = modifier.size(size * 1.5f), contentAlignment = Alignment.Center) {
        // Outer expansion ring 2
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale2)
                .alpha(alpha2)
                .clip(CircleShape)
                .background(color)
        )
        // Outer expansion ring 1
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale1)
                .alpha(alpha1)
                .clip(CircleShape)
                .background(color)
        )
        // Core breathing center
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun DangerGlowBackground(
    modifier: Modifier = Modifier,
    color: Color = MeshTheme.colors.danger,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "danger_glow_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.12f else 0.04f,
        targetValue = if (isActive) 0.28f else 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun LocationPulseIndicator(
    modifier: Modifier = Modifier,
    color: Color = MeshTheme.colors.warning
) {
    val infiniteTransition = rememberInfiniteTransition(label = "location_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "location_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "location_alpha"
    )

    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}
