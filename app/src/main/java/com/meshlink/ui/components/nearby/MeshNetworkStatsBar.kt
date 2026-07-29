package com.meshlink.ui.components.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshNetworkStatsBar(
    devices: List<BleDevice>,
    isScanning: Boolean,
    modifier: Modifier = Modifier
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
            devices.isEmpty() -> if (isScanning) "Scanning" to Color(0xFF2196F3) else "Idle" to Color(0xFF9E9E9E)
            avgRssi > -75 -> "Optimal" to Color(0xFF4CAF50)
            avgRssi > -85 -> "Good" to Color(0xFF2196F3)
            else -> "Fair" to Color(0xFFFF9800)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.extraSmall),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
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

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

            // Nearby Devices
            StatBadge(
                icon = Icons.Default.Radar,
                label = "Nearby",
                value = "$totalNearby"
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

            // Connected
            StatBadge(
                icon = Icons.Default.BluetoothConnected,
                label = "Connected",
                value = "$connectedCount",
                valueColor = if (connectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

            // Relay Nodes
            StatBadge(
                icon = Icons.Default.Hub,
                label = "Relays",
                value = "$relayCount"
            )

            if (avgLatencyMs != null) {
                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                
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
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
