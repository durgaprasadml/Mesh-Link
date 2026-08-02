package com.meshlink.ui.designsystem.components.badges

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

@Composable
fun MeshSignalIndicator(
    rssiDbm: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    val bars = when {
        rssiDbm >= -65 -> 4
        rssiDbm >= -78 -> 3
        rssiDbm >= -90 -> 2
        rssiDbm > -110 -> 1
        else -> 0
    }
    val barColor = when (bars) {
        4, 3 -> colors.signalExcellent
        2 -> colors.signalMedium
        1 -> colors.signalWeak
        else -> colors.textTertiary
    }

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(4) { index ->
            val active = index < bars
            val heightPct = (index + 1) * 0.25f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightPct)
                    .clip(CircleShape)
                    .background(if (active) barColor else colors.border.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun MeshAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isOnline: Boolean = false,
    backgroundColor: Color = LocalMeshSemanticColors.current.primary.copy(alpha = 0.2f)
) {
    val colors = LocalMeshSemanticColors.current
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(1.5.dp, colors.border.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MeshTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            ) {
                MeshStatusDot(color = colors.meshConnected, pulse = true)
            }
        }
    }
}

@Composable
fun MeshStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MeshTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(text = label, style = MeshTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
fun MeshAnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
            } else {
                slideInVertically { height -> -height } + fadeIn() togetherWith
                        slideOutVertically { height -> height } + fadeOut()
            }.using(SizeTransform(clip = false))
        },
        label = "counterAnim",
        modifier = modifier
    ) { targetCount ->
        Text(
            text = targetCount.toString(),
            style = MeshTheme.typography.titleLarge,
            color = colors.textPrimary
        )
    }
}

/**
 * Status chip indicating mesh network connection status.
 */
@Composable
fun MeshConnectedChip(
    peerCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.15f))
            .border(1.dp, colors.primary.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MeshStatusDot(color = colors.meshConnected, pulse = true)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$peerCount ${if (peerCount == 1) "Peer" else "Peers"}",
            style = MeshTheme.typography.labelSmall,
            color = colors.primary
        )
    }
}

/**
 * Emergency status chip for SOS alerts.
 */
@Composable
fun EmergencyIndicatorChip(
    modifier: Modifier = Modifier,
    label: String = "EMERGENCY SOS"
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.emergency.copy(alpha = 0.2f))
            .border(1.dp, colors.emergency.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MeshStatusDot(color = colors.emergency, pulse = true)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MeshTheme.typography.labelSmall,
            color = colors.emergency
        )
    }
}

