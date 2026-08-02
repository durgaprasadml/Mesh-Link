package com.meshlink.ui.broadcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.DeliveryStatus

@Composable
fun DeliveryProgress(
    status: DeliveryStatus,
    modifier: Modifier = Modifier,
    showProgressLine: Boolean = false
) {
    val deliveryState = BroadcastDeliveryState.fromDomain(status)
    val color = Color(deliveryState.colorHex)

    Column(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeliveryStatusIcon(status = status, color = color)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = deliveryState.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
            }
        }

        if (showProgressLine && (status == DeliveryStatus.SENDING || status == DeliveryStatus.RETRYING || status == DeliveryStatus.WAITING_FOR_ACK)) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun DeliveryStatusIcon(
    status: DeliveryStatus,
    color: Color
) {
    when (status) {
        DeliveryStatus.DELIVERED, DeliveryStatus.SEEN -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        DeliveryStatus.SENT, DeliveryStatus.RELAYED -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent to Mesh",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        DeliveryStatus.SENDING, DeliveryStatus.RETRYING, DeliveryStatus.WAITING_FOR_ACK -> {
            val infiniteTransition = rememberInfiniteTransition(label = "sending_rotate")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Broadcasting",
                tint = color,
                modifier = Modifier
                    .size(12.dp)
                    .rotate(rotation)
            )
        }
        DeliveryStatus.PENDING, DeliveryStatus.QUEUED, DeliveryStatus.WAITING_FOR_ROUTE -> {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = "Queued",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        DeliveryStatus.EXPIRED, DeliveryStatus.CANCELLED, DeliveryStatus.PERMANENT_FAILURE, DeliveryStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Delivery Failed",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
