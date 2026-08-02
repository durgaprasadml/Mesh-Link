package com.meshlink.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

enum class ConnectionState {
    CONNECTED,
    SEARCHING,
    NO_DEVICES
}

@Composable
fun ConnectionStatusPill(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, dotColor, text) = when (state) {
        ConnectionState.CONNECTED -> Triple(MeshTheme.colors.success.copy(alpha = 0.15f), MeshTheme.colors.success, "Connected")
        ConnectionState.SEARCHING -> Triple(MeshTheme.colors.warning.copy(alpha = 0.15f), MeshTheme.colors.warning, "Searching Mesh")
        ConnectionState.NO_DEVICES -> Triple(MeshTheme.colors.error.copy(alpha = 0.15f), MeshTheme.colors.error, "Offline")
    }

    val animatedBgColor by animateColorAsState(targetValue = backgroundColor, animationSpec = tween(300), label = "bg_color")
    val animatedDotColor by animateColorAsState(targetValue = dotColor, animationSpec = tween(300), label = "dot_color")

    val infiniteTransition = rememberInfiniteTransition(label = "pill_pulse")

    // Outer ring pulse — used when scanning
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    // Shimmer sweep for SEARCHING state — sweeps alpha across the pill
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedBgColor)
            .border(
                width = 1.dp,
                color = animatedDotColor.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "Connection Status: $text"
            }
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot with animated concentric ring when active
        Box(
            modifier = Modifier.size(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state == ConnectionState.SEARCHING || state == ConnectionState.CONNECTED) {
                // Outer expanding ring
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(animatedDotColor.copy(alpha = if (state == ConnectionState.SEARCHING) 0.30f else 0.25f))
                )
            }
            // Core dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor)
            )
        }
        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "status_text"
        ) { targetText ->
            Text(
                text = targetText,
                color = animatedDotColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
