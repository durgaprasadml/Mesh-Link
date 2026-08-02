package com.meshlink.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.accessibility.rememberMeshReducedMotion
import com.meshlink.ui.designsystem.theme.MeshTheme

enum class MeshOfflineStatus {
    OFFLINE,
    MESH_AVAILABLE,
    WAITING_FOR_PEERS,
    RECONNECTING,
    QUEUE_PENDING,
    SYNC_RUNNING
}

/**
 * Unified Offline Banner & Status Indicator for Mesh-Link.
 * Displays offline states with animated status indicators.
 */
@Composable
fun OfflineStateBanner(
    status: MeshOfflineStatus,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    customMessage: String? = null
) {
    val isReducedMotion = rememberMeshReducedMotion()
    val infiniteTransition = rememberInfiniteTransition(label = "offline_pulse")

    val pulseScale by if (isReducedMotion) {
        androidx.compose.runtime.mutableStateOf(1f)
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    }

    val spinRotation by if (isReducedMotion) {
        androidx.compose.runtime.mutableStateOf(0f)
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
    }

    val (bgContainerColor, contentColor, icon, defaultText) = when (status) {
        MeshOfflineStatus.OFFLINE -> Quadruple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.WifiOff,
            "Offline Mode Active — No cellular or internet"
        )
        MeshOfflineStatus.MESH_AVAILABLE -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CellTower,
            "Mesh Network Active — Peer-to-peer routing enabled"
        )
        MeshOfflineStatus.WAITING_FOR_PEERS -> Quadruple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.HourglassTop,
            "Waiting for nearby Mesh-Link peers..."
        )
        MeshOfflineStatus.RECONNECTING -> Quadruple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Refresh,
            "Reconnecting to mesh nodes..."
        )
        MeshOfflineStatus.QUEUE_PENDING -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Public,
            "Messages queued — Will deliver when peer connects"
        )
        MeshOfflineStatus.SYNC_RUNNING -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CloudSync,
            "Syncing offline payloads with mesh topology..."
        )
    }

    val animatedBg by animateColorAsState(targetValue = bgContainerColor, label = "bg_anim")

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = animatedBg,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .then(
                                if (status == MeshOfflineStatus.RECONNECTING || status == MeshOfflineStatus.SYNC_RUNNING) {
                                    Modifier.rotate(spinRotation)
                                } else Modifier
                            )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = customMessage ?: defaultText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
