package com.meshlink.ui.designsystem.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.MeshColorTokens

@Composable
fun SignalMeter(
    rssiDbm: Int,
    modifier: Modifier = Modifier
) {
    val (signalColor, barsActive) = when {
        rssiDbm >= -65 -> Pair(MeshTheme.colors.connected, 4)
        rssiDbm >= -78 -> Pair(MeshTheme.colors.connected, 3)
        rssiDbm >= -90 -> Pair(MeshTheme.colors.warning, 2)
        else -> Pair(MeshTheme.colors.danger, 1)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            val barHeight = (i * 4 + 2).dp
            val isActive = i <= barsActive
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(MeshTheme.shapes.tiny)
                    .background(if (isActive) signalColor else MeshTheme.colors.border)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$rssiDbm dBm",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = signalColor
        )
    }
}

@Composable
fun RSSIMeter(rssiDbm: Int, modifier: Modifier = Modifier) {
    SignalMeter(rssiDbm = rssiDbm, modifier = modifier)
}

@Composable
fun HopBadge(
    hopCount: Int,
    modifier: Modifier = Modifier
) {
    val label = if (hopCount == 1) "1 HOP (DIRECT)" else "$hopCount HOPS"
    val color = if (hopCount == 1) MeshTheme.colors.primary else MeshTheme.colors.secondary

    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.pill)
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.4f), MeshTheme.shapes.pill)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun MeshStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    color: Color = MeshTheme.colors.primary
) {
    Row(
        modifier = modifier
            .clip(MeshTheme.shapes.pill)
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.4f), MeshTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(MeshTheme.shapes.circular)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MeshStatusPill(
    status: String,
    modifier: Modifier = Modifier,
    color: Color = MeshTheme.colors.primary
) {
    MeshStatusBadge(status = status, modifier = modifier, color = color)
}
