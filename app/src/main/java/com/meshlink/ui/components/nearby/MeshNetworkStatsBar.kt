package com.meshlink.ui.components.nearby

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshNetworkStatsBar(
    devices: List<BleDevice>,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    packetCount: Int = 0
) {
    val totalNearby = devices.size
    val connectedCount = devices.count { it.isConnected }
    val relayCount = devices.count { (it.capabilities.toInt() and 0x01 != 0) || (it.isConnected && it.rssi > -75) }
    
    val avgRssi = remember(devices) {
        if (devices.isEmpty()) 0 else devices.map { it.rssi }.average().toInt()
    }
    
    val avgLatencyMs = remember(devices) {
        if (devices.isEmpty()) null else (-avgRssi * 0.35).toInt().coerceIn(8, 120)
    }

    val semanticColors = MeshTheme.colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    val (healthText, healthColor) = remember(devices, isScanning, avgRssi, primaryColor, secondaryColor, outlineColor) {
        when {
            devices.isEmpty() -> if (isScanning) "Scanning" to secondaryColor else "Idle" to outlineColor
            avgRssi > -75 -> "Optimal" to semanticColors.signalStrong
            avgRssi > -85 -> "Good" to semanticColors.signalMedium
            else -> "Fair" to semanticColors.signalWeak
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.extraSmall),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MeshTheme.shapes.large,
        tonalElevation = MeshTheme.elevation.level1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health Badge
            StatBadge(
                icon = Icons.Default.Security,
                label = "Health",
                value = healthText,
                valueColor = healthColor
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Nearby Devices
            StatBadge(
                icon = Icons.Default.Radar,
                label = "Nearby",
                value = "$totalNearby"
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Connected
            StatBadge(
                icon = Icons.Default.BluetoothConnected,
                label = "Connected",
                value = "$connectedCount",
                valueColor = if (connectedCount > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Relay Nodes
            StatBadge(
                icon = Icons.Default.Hub,
                label = "Relays",
                value = "$relayCount"
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Packet Counter
            StatBadge(
                icon = Icons.Default.SwapHoriz,
                label = "Packets",
                value = "$packetCount",
                valueColor = if (packetCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (avgLatencyMs != null) {
                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                // Latency
                StatBadge(
                    icon = Icons.Default.Speed,
                    label = "Avg Latency",
                    value = "~${avgLatencyMs} ms"
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun StatBadge(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                fadeIn() + slideInVertically { height -> height } togetherWith fadeOut() + slideOutVertically { height -> -height }
            },
            label = "StatValueAnimation"
        ) { animatedValue ->
            Text(
                text = animatedValue,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
