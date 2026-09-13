package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.SuccessColorDark

/**
 * Modern, responsive Mesh Network Status Bar.
 *
 * Displays live, verified mesh metrics across 4 balanced columns without horizontal clipping:
 * - Discovery Status: Live scanning / Paused state with subtle pulse dot
 * - Nearby Peers: Total verified nearby peers discovered
 * - Connected: Count of active peer connections
 * - Transport: Active physical transport (BLE / Wi-Fi Direct)
 *
 * Free of fake metrics, synthetic latency estimates, or arbitrary heuristics.
 */
@Composable
fun MeshNetworkStatsBar(
    devices: List<BleDevice>,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val totalNearby = devices.size
    val connectedCount = remember(devices) { devices.count { it.isConnected } }
    
    val hasWifiDirect = remember(devices) {
        devices.any { it.transport == TransportType.WIFI_DIRECT }
    }
    val transportLabel = if (hasWifiDirect) "BLE + P2P" else "BLE"

    val statusText = if (isScanning) "Scanning" else "Paused"
    val statusColor = if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "StatsPulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatusDotPulse"
    )

    val contentDesc = "Network summary: $statusText, $totalNearby nearby peers, $connectedCount connected, transport $transportLabel"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.small)
            .semantics { contentDescription = contentDesc },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.mediumSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Discovery State
            MetricItem(
                label = "Discovery",
                value = statusText,
                valueColor = statusColor,
                leadingDot = {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isScanning) {
                                    statusColor.copy(alpha = pulseAlpha)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                }
                            )
                    )
                },
                modifier = Modifier.weight(1f)
            )

            // 2. Nearby Count
            MetricItem(
                label = "Nearby",
                value = "$totalNearby",
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.85f)
            )

            // 3. Connected Count
            MetricItem(
                label = "Connected",
                value = "$connectedCount",
                valueColor = if (connectedCount > 0) SuccessColorDark else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.95f)
            )

            // 4. Transport Mode
            MetricItem(
                label = "Transport",
                value = transportLabel,
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.95f)
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    leadingDot: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            leadingDot?.invoke()
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
