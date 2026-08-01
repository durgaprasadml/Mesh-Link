package com.meshlink.ui.designsystem.components.badges

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.meshlink.ui.designsystem.theme.BrandPrimary
import com.meshlink.ui.designsystem.theme.BrandSecondary

/**
 * Animated Pulsing Online/Active Node Indicator Dot.
 */
@Composable
fun PulsingNodeDot(
    modifier: Modifier = Modifier,
    color: Color = BrandPrimary,
    size: Dp = 10.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 1.8f)
    ) {
        // Outer glowing pulse ring
        Box(
            modifier = Modifier
                .size(size * pulseScale)
                .clip(CircleShape)
                .background(color.copy(alpha = pulseAlpha))
        )
        // Solid core dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Hop Count & Connection Route Badge (e.g. "Direct BLE" or "2 Hops").
 */
@Composable
fun HopRouteBadge(
    hopCount: Int,
    modifier: Modifier = Modifier,
    isDirect: Boolean = hopCount <= 1
) {
    val badgeColor = if (isDirect) BrandPrimary else BrandSecondary
    val badgeText = if (isDirect) "Direct" else "$hopCount Hops"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .border(0.75.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = badgeColor
        )
    }
}

/**
 * Signal Strength RSSI Bar Meter Component.
 */
@Composable
fun SignalRssiMeter(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -70 -> 3
        rssi >= -85 -> 2
        else -> 1
    }
    val meterColor = when (bars) {
        4 -> BrandPrimary
        3 -> BrandSecondary
        2 -> Color(0xFFFF9F0A)
        else -> Color(0xFFFF453A)
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.height(12.dp)
    ) {
        for (i in 1..4) {
            val barHeight = (i * 3).dp
            val isFilled = i <= bars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isFilled) meterColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )
        }
    }
}
