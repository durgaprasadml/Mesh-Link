package com.meshlink.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.home.ConnectionState

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

    val animatedBackgroundColor by animateColorAsState(targetValue = backgroundColor, animationSpec = tween(300), label = "bg_color")
    val animatedDotColor by animateColorAsState(targetValue = dotColor, animationSpec = tween(300), label = "dot_color")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedBackgroundColor)
            .border(
                width = 1.dp,
                color = animatedDotColor.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "Connection Status: $text"
            }
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state == ConnectionState.SEARCHING || state == ConnectionState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(animatedDotColor.copy(alpha = 0.35f))
                )
            }
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
