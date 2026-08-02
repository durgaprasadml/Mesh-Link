package com.meshlink.ui.broadcast

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object BroadcastAnimations {
    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SpringFast = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val FadeSpec = tween<Float>(durationMillis = 220, easing = LinearOutSlowInEasing)
}

/**
 * Animated Emergency Beacon Pulse effect.
 */
@Composable
fun EmergencyBeaconPulse(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF0055),
    size: Dp = 12.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_beacon")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_alpha"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier.background(color, CircleShape)
        )
    }
}

/**
 * Animated Sending Wave / Ripple Effect for Broadcast Cards or Composer.
 */
@Composable
fun BroadcastSendRipple(
    modifier: Modifier = Modifier,
    isBroadcasting: Boolean = false,
    color: Color = Color(0xFF00E5FF)
) {
    if (!isBroadcasting) return

    val infiniteTransition = rememberInfiniteTransition(label = "broadcast_ripple")
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple_alpha"
    )

    Box(
        modifier = modifier
            .border(1.5.dp, color.copy(alpha = pulseAlpha), CircleShape)
    )
}
