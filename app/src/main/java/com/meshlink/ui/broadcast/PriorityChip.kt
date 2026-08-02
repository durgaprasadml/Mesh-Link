package com.meshlink.ui.broadcast

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.scaleOnPress

@Composable
fun PriorityChip(
    priority: BroadcastPriority,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val containerColor = Color(priority.containerColor)
    val badgeColor = Color(priority.badgeColor)

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor),
        modifier = modifier
            .then(
                if (onClick != null) Modifier
                    .scaleOnPress(0.95f)
                    .clickable { onClick() }
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (priority.isEmergency) {
                EmergencyBeaconPulse(size = if (compact) 6.dp else 8.dp)
                Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))
            }

            Text(
                text = priority.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = badgeColor
            )
        }
    }
}

@Composable
fun EmergencyBeaconPulse(
    size: Dp = 8.dp,
    color: Color = Color(0xFFFF0055)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
