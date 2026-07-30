package com.meshlink.ui.components.nearby

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val (healthText, healthColor) = remember(devices, isScanning, avgRssi) {
        when {
            devices.isEmpty() -> if (isScanning) "Scanning" to Color(0xFF00B0FF) else "Idle" to Color(0xFF9E9E9E)
            avgRssi > -75 -> "Optimal" to Color(0xFF00E676)
            avgRssi > -85 -> "Good" to Color(0xFF00B0FF)
            else -> "Fair" to Color(0xFFFF9800)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.extraSmall),
        color = Color(0xFF141414).copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
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

            VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.15f))

            // Nearby Devices
            StatBadge(
                icon = Icons.Default.Radar,
                label = "Nearby",
                value = "$totalNearby"
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.15f))

            // Connected
            StatBadge(
                icon = Icons.Default.BluetoothConnected,
                label = "Connected",
                value = "$connectedCount",
                valueColor = if (connectedCount > 0) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f)
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.15f))

            // Relay Nodes
            StatBadge(
                icon = Icons.Default.Hub,
                label = "Relays",
                value = "$relayCount"
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.15f))

            // Packet Counter
            StatBadge(
                icon = Icons.Default.SwapHoriz,
                label = "Packets",
                value = "$packetCount",
                valueColor = if (packetCount > 0) Color(0xFF00FF88) else Color.White.copy(alpha = 0.6f)
            )

            if (avgLatencyMs != null) {
                VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.15f))
                
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
    valueColor: Color = Color.White
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
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
