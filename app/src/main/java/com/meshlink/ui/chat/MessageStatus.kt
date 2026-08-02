package com.meshlink.ui.chat

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.DeliveryStatus

/**
 * Material 3 Message Status indicator composable.
 * Displays animated status icons (Sending, Sent, Delivered, Read, Failed) with smooth transitions.
 */
@Composable
fun MessageStatusIcon(
    status: DeliveryStatus,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    val deliveryState = remember(status) { DeliveryState.fromDomain(status) }
    
    val animatedTint by animateColorAsState(
        targetValue = when (deliveryState) {
            DeliveryState.READ -> MaterialTheme.colorScheme.primary
            DeliveryState.FAILED -> MaterialTheme.colorScheme.error
            else -> tint
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "statusTintAnimation"
    )

    AnimatedContent(
        targetState = deliveryState,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "messageStatusTransition",
        modifier = modifier
    ) { state ->
        when (state) {
            DeliveryState.PENDING, DeliveryState.SENDING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "sendingSpinner")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Sending",
                    tint = animatedTint,
                    modifier = Modifier
                        .size(iconSize)
                        .rotate(angle)
                )
            }
            DeliveryState.SENT -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sent",
                    tint = animatedTint,
                    modifier = Modifier.size(iconSize)
                )
            }
            DeliveryState.DELIVERED, DeliveryState.RELAYED -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Delivered",
                    tint = animatedTint,
                    modifier = Modifier.size(iconSize)
                )
            }
            DeliveryState.READ -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Read",
                    tint = animatedTint,
                    modifier = Modifier.size(iconSize)
                )
            }
            DeliveryState.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = animatedTint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun MessageStatusWithTime(
    timestampText: String,
    status: DeliveryStatus,
    isOutbound: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 2.dp)
    ) {
        Text(
            text = timestampText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        if (isOutbound) {
            Spacer(modifier = Modifier.width(4.dp))
            MessageStatusIcon(status = status, iconSize = 13.dp)
        }
    }
}
